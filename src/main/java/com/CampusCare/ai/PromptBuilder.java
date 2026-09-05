package com.CampusCare.ai;

import org.springframework.stereotype.Component;

@Component
public class PromptBuilder {

	public String build(AIRequest request) {
		String additional = request.getAdditionalInformation() == null
				? "None"
				: request.getAdditionalInformation();

		return """
				Analyze this campus complaint.
				Return ONLY valid JSON with exactly these fields:
				{"sufficientInformation":true|false,"followUpQuestion":string|null,"department":string|null}
				Department must be one of: ELECTRICAL, PLUMBING, IT, CLEANING, CIVIL, HOSTEL.
				If information is insufficient, ask one simple follow-up question and set department to null.
				If sufficient, set followUpQuestion to null and select the best department.
				Title: %s
				Description: %s
				Additional information: %s
				""".formatted(request.getTitle(), request.getDescription(), additional);
	}
}
