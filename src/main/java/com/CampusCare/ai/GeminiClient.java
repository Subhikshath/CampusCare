package com.CampusCare.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class GeminiClient {

	private static final Logger logger = LoggerFactory.getLogger(GeminiClient.class);
	private static final int MAX_UNAVAILABLE_RETRIES = 2;
	private final ObjectMapper objectMapper;

	@Value("${gemini.api-key}")
	private String apiKey;

	@Value("${gemini.model}")
	private String model;

	@Value("${gemini.mock:false}")
	private boolean mock;

	@PostConstruct
	void logConfiguration() {
		if (mock) {
			logger.info("Gemini mock mode is active");
		} else if (apiKey == null || apiKey.isBlank()) {
			logger.warn("GEMINI_API_KEY is not configured; Gemini requests will fail");
		} else {
			logger.info("Gemini API key is configured for model {}", model);
		}
	}

	@SuppressWarnings("null")
	public AIResponse analyze(String prompt, String studentText) {
		if (mock) {
			String location = CampusLocationValidator.findMockLocation(studentText);
			if (location == null) {
				return new AIResponse(false,
						"Where exactly on campus is the issue? Please share the named building or block and, where relevant, its floor or room number; for outdoor issues, name the ground or parking area.",
						null, null);
			}
			return new AIResponse(true, null, mockDepartment(studentText), location);
		}
		if (apiKey == null || apiKey.isBlank()) {
			throw new IllegalStateException("GEMINI_API_KEY is not configured");
		}

		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(5000);
		requestFactory.setReadTimeout(45000);
		String requestBody;
		try {
			requestBody = objectMapper.writeValueAsString(Map.of(
					"contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
					"generationConfig", Map.of(
							"responseMimeType", "application/json",
							"maxOutputTokens", 2048)));
		} catch (Exception exception) {
			throw new IllegalStateException("Unable to encode Gemini request", exception);
		}
		JsonNode root = generateContent(requestBody, requestFactory);
		JsonNode candidate = root.path("candidates").path(0);
		String finishReason = candidate.path("finishReason").asText();
		String text = candidate.path("content").path("parts").path(0)
				.path("text").asText();
		if ("MAX_TOKENS".equals(finishReason)) {
			logger.warn("Gemini response was truncated at the output token limit");
			throw new AIServiceException(
					"Gemini returned an incomplete response. Please submit again.", null);
		}
		if (text.isBlank()) {
			throw new AIServiceException(
					"Gemini returned an empty response. Please submit again.", null);
		}
		try {
			JsonNode result = objectMapper.readTree(stripMarkdown(text));
			if (!result.path("sufficientInformation").isBoolean()
					|| (result.hasNonNull("followUpQuestion")
							&& !result.path("followUpQuestion").isTextual())
					|| (result.hasNonNull("department")
							&& !result.path("department").isTextual())
					|| (result.hasNonNull("campusLocation")
							&& !result.path("campusLocation").isTextual())) {
				throw new AIServiceException(
						"Gemini returned an invalid response. Please submit again.", null);
			}

			return new AIResponse(
					result.path("sufficientInformation").asBoolean(),
					nullableText(result, "followUpQuestion"),
					nullableText(result, "department"),
					nullableText(result, "campusLocation"));
		} catch (JsonProcessingException exception) {
			logger.warn("Gemini returned invalid JSON; finishReason={}", finishReason);
			throw new AIServiceException(
					"Gemini returned an incomplete response. Please submit again.", exception);
		}
	}

	@SuppressWarnings("null")
	public String chat(String prompt) {
		if (apiKey == null || apiKey.isBlank()) {
			throw new IllegalStateException("GEMINI_API_KEY is not configured");
		}
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(5000);
		requestFactory.setReadTimeout(45000);
		String requestBody;
		try {
			requestBody = objectMapper.writeValueAsString(Map.of(
					"contents", List.of(Map.of("parts", List.of(Map.of("text",
							prompt)))),
					"generationConfig", Map.of("maxOutputTokens", 512)));
		} catch (Exception exception) {
			throw new IllegalStateException("Unable to encode Gemini request", exception);
		}
		try {
			JsonNode root = generateContent(requestBody, requestFactory);
			String reply = root == null ? null : root.path("candidates").path(0)
					.path("content").path("parts").path(0).path("text").asText(null);
			if (reply == null || reply.isBlank()) {
				throw new IllegalStateException("Gemini returned an empty chat response");
			}
			return reply.trim();
		} catch (IllegalStateException exception) {
			throw exception;
		} catch (Exception exception) {
			throw new IllegalStateException("Gemini chat request failed", exception);
		}
	}

	@SuppressWarnings("null")
	private JsonNode generateContent(String requestBody,
			SimpleClientHttpRequestFactory requestFactory) {
		for (int attempt = 0; ; attempt++) {
			try {
				JsonNode response = RestClient.builder()
						.requestFactory(requestFactory)
						.build()
						.post()
						.uri("https://generativelanguage.googleapis.com/v1beta/models/"
								+ model + ":generateContent")
						.header("x-goog-api-key", apiKey)
						.contentType(MediaType.APPLICATION_JSON)
						.body(requestBody)
						.retrieve()
						.body(JsonNode.class);
				if (response == null) {
					throw new AIServiceException("Gemini returned an empty response.", null);
				}
				return response;
			} catch (RestClientResponseException exception) {
				HttpStatus status = HttpStatus.resolve(exception.getStatusCode().value());
				if (status == HttpStatus.SERVICE_UNAVAILABLE
						&& attempt < MAX_UNAVAILABLE_RETRIES) {
					long delayMillis = 500L << attempt;
					logger.warn("Gemini is unavailable; retrying request (attempt {}/{})",
							attempt + 1, MAX_UNAVAILABLE_RETRIES);
					try {
						Thread.sleep(delayMillis);
					} catch (InterruptedException interrupted) {
						Thread.currentThread().interrupt();
						throw new AIServiceException(
								"Gemini retry was interrupted. Please try again.", interrupted);
					}
					continue;
				}
				logger.warn("Gemini returned HTTP {}", exception.getStatusCode().value());
				throw new AIServiceException(
						errorMessage(status, exception.getResponseBodyAsString()), exception);
			}
			catch (ResourceAccessException exception) {
				logger.warn("Gemini network request failed: {}",
						exception.getClass().getSimpleName());
				throw new AIServiceException(
						"CampusCare could not reach Gemini. Check the network connection and try again.",
						exception);
			} catch (RestClientException exception) {
				logger.warn("Gemini client request failed: {}",
						exception.getClass().getSimpleName());
				throw new AIServiceException(
						"Gemini could not process the request right now. Try again later.",
						exception);
			}
		}
	}

	String errorMessage(HttpStatus status, String responseBody) {
		JsonNode error;
		try {
			JsonNode root = objectMapper.readTree(responseBody);
			error = root == null ? objectMapper.createObjectNode() : root.path("error");
		} catch (JsonProcessingException | IllegalArgumentException ignored) {
			error = objectMapper.createObjectNode();
		}
		String providerDetails = error.findValuesAsText("reason").stream()
				.collect(Collectors.joining(" "))
				+ " " + error.path("status").asText("")
				+ " " + error.path("message").asText("");
		String details = providerDetails.toUpperCase(Locale.ROOT);

		if (status == HttpStatus.UNAUTHORIZED || details.contains("API_KEY_INVALID")) {
			return "Gemini rejected GEMINI_API_KEY. Create or select a valid Gemini API key and update the backend environment.";
		}
		if (status == HttpStatus.FORBIDDEN) {
			if (details.contains("SERVICE_DISABLED")
					|| details.contains("SERVICE_NOT_ACTIVATED")
					|| details.contains("API_NOT_ENABLED")) {
				return "The Generative Language API is disabled for the Google Cloud project linked to this key. Enable it for that project, then restart CampusCare.";
			}
			if (details.contains("API_KEY_SERVICE_BLOCKED")
					|| details.contains("API_KEY_IP_ADDRESS_BLOCKED")
					|| details.contains("API_KEY_HTTP_REFERRER_BLOCKED")
					|| details.contains("REFERER")
					|| details.contains("REFERRER")
					|| details.contains("IP ADDRESS")) {
				return "Google blocked this server request because of the API key restrictions. Allow the Generative Language API and remove browser-only referrer restrictions for this server key.";
			}
			return "Google denied Gemini access. Check that the key belongs to a project with the Generative Language API enabled and permits this server request. Since May 28, 2026, unrestricted standard Gemini keys are rejected; use an AI Studio authorization key or restrict the standard key to the Gemini API.";
		}
		if (status == HttpStatus.NOT_FOUND) {
			return "The configured Gemini model is unavailable. Check GEMINI_MODEL against Google's supported Gemini API models.";
		}
		if (status == HttpStatus.TOO_MANY_REQUESTS) {
			return "Gemini's request quota or rate limit has been reached. Check the project's Gemini API quota and billing, then try again later.";
		}
		if (status == HttpStatus.SERVICE_UNAVAILABLE) {
			return "Gemini is temporarily unavailable or the model is overloaded. CampusCare retried the request; please try again shortly.";
		}
		if (status != null && status.is5xxServerError()) {
			return "Gemini is temporarily unavailable. Please try again shortly.";
		}
		return "Gemini could not process the request. Check the API key, project access, model, and request configuration.";
	}

	private String stripMarkdown(String value) {
		return value.replace("```json", "").replace("```", "").trim();
	}

	private String nullableText(JsonNode node, String field) {
		JsonNode value = node.get(field);
		return value == null || value.isNull() ? null : value.asText();
	}

	private String mockDepartment(String studentText) {
		String text = studentText.toLowerCase();
		if (text.matches("(?s).*\\b(power|electricity|electrical|light|outlet|wiring)\\b.*")) {
			return "ELECTRICAL_MAINTENANCE";
		}
		if (text.matches("(?s).*\\b(water|leak|pipe|tap|drain|toilet)\\b.*")) {
			return "PLUMBING";
		}
		if (text.matches("(?s).*\\b(wi-fi|wifi|network|computer|projector)\\b.*")) {
			return "IT_SUPPORT";
		}
		if (text.matches("(?s).*\\b(clean|cleaning|trash|garbage|spill|waste)\\b.*")) {
			return "HOUSEKEEPING";
		}
		return "ADMINISTRATION";
	}
}
