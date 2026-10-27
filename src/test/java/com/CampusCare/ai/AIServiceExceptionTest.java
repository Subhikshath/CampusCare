package com.CampusCare.ai;

import com.CampusCare.common.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AIServiceExceptionTest {

    @Test
    void reportsUnavailableGeminiAsServiceUnavailableWithSafeMessage() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/student/complaints");
        AIServiceException exception = new AIServiceException(
                "Gemini rejected the configured API key. Create a replacement key and update GEMINI_API_KEY.",
                new IllegalStateException("provider detail"));

        var response = handler.aiUnavailable(exception, request);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals(exception.getMessage(), response.getBody().message());
    }
}
