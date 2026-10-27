package com.CampusCare.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AIServiceTest {

    @Test
    void rejectsIncompleteResponseWithoutFollowUpQuestion() {
        GeminiClient client = mock(GeminiClient.class);
        when(client.analyze(anyString(), anyString()))
                .thenReturn(new AIResponse(false, null, null, "Block A, room 204"));
        AIService service = service(client);

        AIRequest request = request();
        request.setDescription("Water leak in Block A, room 204");

        assertThrows(IllegalStateException.class, () -> service.analyze(request));
    }

    @Test
    void acceptsCompleteResponseWithSpecificStudentProvidedLocation() {
        GeminiClient client = mock(GeminiClient.class);
        when(client.analyze(anyString(), anyString())).thenReturn(
                new AIResponse(true, null, "PLUMBING", "Block A, floor 2, room 204"));

        AIRequest request = request();
        request.setDescription("Water leak in Block A, floor 2, room 204.");

        AIResponse response = service(client).analyze(request);

        assertEquals(true, response.isSufficientInformation());
        assertEquals("PLUMBING", response.getDepartment());
    }

    @Test
    void asksForLocationInsteadOfRoutingAnUnderSpecifiedReport() {
        GeminiClient client = mock(GeminiClient.class);
        when(client.analyze(anyString(), anyString())).thenReturn(
                new AIResponse(true, null, "ELECTRICAL_MAINTENANCE", null));

        AIResponse response = service(client).analyze(request());

        assertEquals(false, response.isSufficientInformation());
        assertEquals(null, response.getDepartment());
        assertEquals(true, response.getFollowUpQuestion().contains("Where exactly on campus"));
    }

    @Test
    void asksForLocationBeforeAcceptingAnUnrelatedFollowUpQuestion() {
        GeminiClient client = mock(GeminiClient.class);
        when(client.analyze(anyString(), anyString())).thenReturn(
                new AIResponse(false, "When did this problem start?", null, null));

        AIResponse response = service(client).analyze(request());

        assertEquals(false, response.isSufficientInformation());
        assertEquals(null, response.getDepartment());
        assertEquals(true, response.getFollowUpQuestion().contains("Where exactly on campus"));
    }

    @Test
    void doesNotAcceptGenericOrInventedLocationEvenIfTheModelTriesToRoute() {
        GeminiClient client = mock(GeminiClient.class);
        when(client.analyze(anyString(), anyString())).thenReturn(
                new AIResponse(true, null, "PLUMBING", "the campus"));

        AIRequest request = request();
        request.setAdditionalInformation("AI: Where is the issue? The campus.");

        AIResponse response = service(client).analyze(request);

        assertEquals(false, response.isSufficientInformation());
        assertEquals(null, response.getDepartment());
    }

    private AIService service(GeminiClient client) {
        return new AIService(client, new PromptBuilder(), new CampusLocationValidator());
    }

    private AIRequest request() {
        AIRequest request = new AIRequest();
        request.setTitle("Water leak");
        request.setDescription("Water is leaking");
        return request;
    }
}
