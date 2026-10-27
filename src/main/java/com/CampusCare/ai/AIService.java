package com.CampusCare.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AIService {

	private final GeminiClient geminiClient;
	private final PromptBuilder promptBuilder;
	private final CampusLocationValidator campusLocationValidator;

	public AIResponse analyze(AIRequest request) {
		String studentText = studentText(request);
		AIResponse response = geminiClient.analyze(promptBuilder.build(request), studentText);
		if (!campusLocationValidator.isSpecificLocation(
				response.getCampusLocation(), studentText)) {
			return new AIResponse(false, campusLocationValidator.studentLocationFollowUp(),
					null, response.getCampusLocation());
		}
		if (!response.isSufficientInformation()
				&& (response.getFollowUpQuestion() == null || response.getFollowUpQuestion().isBlank())) {
			throw new IllegalStateException("Gemini returned no follow-up question for incomplete information");
		}
		if (response.isSufficientInformation()
				&& (response.getDepartment() == null || response.getDepartment().isBlank())) {
			throw new IllegalStateException("Gemini returned no department for complete information");
		}
		if (!response.isSufficientInformation() && response.getDepartment() != null
				&& !response.getDepartment().isBlank()) {
			throw new IllegalStateException("Gemini returned a department before information was complete");
		}
		return response;
	}

	private String studentText(AIRequest request) {
		String conversation = request.getAdditionalInformation();
		if (conversation == null) {
			conversation = "";
		} else {
			conversation = conversation.replaceAll("(?im)^AI:\\s*[^\\r\\n]*", " ");
		}
		return String.join(" ", request.getTitle(), request.getDescription(), conversation);
	}
}
