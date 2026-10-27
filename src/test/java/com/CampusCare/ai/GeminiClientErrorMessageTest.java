package com.CampusCare.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GeminiClientErrorMessageTest {

	private final GeminiClient client = new GeminiClient(new ObjectMapper());

	@Test
	void explainsDisabledGenerativeLanguageApi() {
		String message = client.errorMessage(HttpStatus.FORBIDDEN, """
				{"error":{"status":"PERMISSION_DENIED","details":[
				  {"reason":"SERVICE_DISABLED"}
				]}}
				""");

		assertTrue(message.contains("Generative Language API is disabled"));
	}

	@Test
	void explainsServerIncompatibleApiKeyRestrictions() {
		String message = client.errorMessage(HttpStatus.FORBIDDEN, """
				{"error":{"details":[{"reason":"API_KEY_HTTP_REFERRER_BLOCKED"}]}}
				""");

		assertTrue(message.contains("browser-only referrer restrictions"));
	}

	@Test
	void providesCurrentGeminiKeyGuidanceForUnclassifiedForbiddenResponses() {
		String message = client.errorMessage(HttpStatus.FORBIDDEN, """
				{"error":{"status":"PERMISSION_DENIED","message":"Request denied"}}
				""");

		assertTrue(message.contains("unrestricted standard Gemini keys are rejected"));
	}

	@Test
	void explainsTemporaryModelOverload() {
		String message = client.errorMessage(HttpStatus.SERVICE_UNAVAILABLE, """
				{"error":{"status":"UNAVAILABLE"}}
				""");

		assertTrue(message.contains("model is overloaded"));
		assertTrue(message.contains("retried the request"));
	}

	@Test
	void explainsQuotaAndRateLimits() {
		String message = client.errorMessage(HttpStatus.TOO_MANY_REQUESTS, "");

		assertTrue(message.contains("quota or rate limit"));
	}
}
