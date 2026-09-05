package com.CampusCare.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AIService {

	private final GeminiClient geminiClient;
	private final PromptBuilder promptBuilder;

	public AIResponse analyze(AIRequest request) {
		return geminiClient.analyze(promptBuilder.build(request));
	}
}
