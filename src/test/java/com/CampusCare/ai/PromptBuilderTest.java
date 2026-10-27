package com.CampusCare.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PromptBuilderTest {

    @Test
    void includesTheCompleteConversationForFollowUpAnalysis() {
        AIRequest request = new AIRequest();
        request.setTitle("Broken light");
        request.setDescription("A light is not working.");
        request.setAdditionalInformation(
                "STUDENT: It is near the east stairs.\nAI: When did you notice it?\nSTUDENT: Yesterday.");

        String prompt = new PromptBuilder().build(request);

        assertTrue(prompt.contains("STUDENT: It is near the east stairs."));
        assertTrue(prompt.contains("AI: When did you notice it?"));
        assertTrue(prompt.contains("STUDENT: Yesterday."));
    }

    @Test
    void requiresActionableIssueAndSpecificCampusLocationBeforeRouting() {
        AIRequest request = new AIRequest();
        request.setTitle("Issue");
        request.setDescription("Something is broken");

        String prompt = new PromptBuilder().build(request);

        assertTrue(prompt.contains("\"campusLocation\":string|null"));
        assertTrue(prompt.contains("\"power cut\" is not enough"));
        assertTrue(prompt.contains("get the hostel name"));
        assertTrue(prompt.contains("get the named ground/field or parking lot"));
        assertTrue(prompt.contains("never invent, infer, or copy a location"));
        assertTrue(prompt.contains("never route it"));
        assertTrue(prompt.contains("TRANSPORT_SERVICES"));
        assertTrue(prompt.contains("LIBRARY_SERVICES"));
        assertTrue(prompt.contains("LABORATORY_MANAGEMENT"));
        assertTrue(prompt.contains("SPORTS_AND_RECREATION"));
        assertTrue(prompt.contains("HOSTEL_ADMINISTRATION"));
        assertTrue(prompt.contains("CANTEEN_AND_FOOD_SERVICES"));
    }

    @Test
    void mockLocationDetectionRequiresARealNamedCampusPlace() {
        assertNull(CampusLocationValidator.findMockLocation("current cut, power cut"));
        assertTrue(CampusLocationValidator.findMockLocation("the issue is in Block A") != null);
        assertNull(CampusLocationValidator.findMockLocation("the issue is in the hostel"));
        assertTrue(CampusLocationValidator.findMockLocation("the issue is in hostel 3") != null);
    }

    @Test
    void locationValidationRejectsRoomsWithoutBuildingsAndPlaceholderNames() {
        CampusLocationValidator validator = new CampusLocationValidator();

        assertFalse(validator.isSpecificLocation("Room 204", "The leak is in Room 204."));
        assertFalse(validator.isSpecificLocation("Block name", "The leak is in Block name."));
        assertFalse(validator.isSpecificLocation("The campus", "The leak is on the campus."));
        assertTrue(validator.isSpecificLocation(
                "Block A, floor 2, room 204",
                "The leak is in Block A, floor 2, room 204."));
        assertTrue(validator.isSpecificLocation(
                "Football ground", "The lights are out at the Football ground."));
    }
}
