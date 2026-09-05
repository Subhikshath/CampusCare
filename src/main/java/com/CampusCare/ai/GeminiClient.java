package com.CampusCare.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class GeminiClient {

	private final ObjectMapper objectMapper;

	@Value("${gemini.api-key}")
	private String apiKey;

	@Value("${gemini.model}")
	private String model;

	public AIResponse analyze(String prompt) {
		if (apiKey == null || apiKey.isBlank()) {
			throw new IllegalStateException("GEMINI_API_KEY is not configured");
		}

		String response = RestClient.create()
				.post()
				.uri("https://generativelanguage.googleapis.com/v1beta/models/"
						+ model + ":generateContent?key=" + apiKey)
				.contentType(MediaType.APPLICATION_JSON)
				.body("{\"contents\":[{\"parts\":[{\"text\":"
						+ quote(prompt) + "}]}]}")
				.retrieve()
				.body(String.class);

		try {
			JsonNode root = objectMapper.readTree(response);
			String text = root.path("candidates").path(0)
					.path("content").path("parts").path(0)
					.path("text").asText();
			JsonNode result = objectMapper.readTree(stripMarkdown(text));

			return new AIResponse(
					result.path("sufficientInformation").asBoolean(),
					nullableText(result, "followUpQuestion"),
					nullableText(result, "department"));
		} catch (Exception exception) {
			throw new IllegalStateException("Gemini returned an invalid response", exception);
		}
	}

	private String quote(String value) {
		try {
			return objectMapper.writeValueAsString(value);
		} catch (Exception exception) {
			throw new IllegalStateException("Unable to encode Gemini prompt", exception);
		}
	}

	private String stripMarkdown(String value) {
		return value.replace("```json", "").replace("```", "").trim();
	}

	private String nullableText(JsonNode node, String field) {
		JsonNode value = node.get(field);
		return value == null || value.isNull() ? null : value.asText();
	}
}
