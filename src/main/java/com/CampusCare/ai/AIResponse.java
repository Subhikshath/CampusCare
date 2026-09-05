package com.CampusCare.ai;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AIResponse {

	private boolean sufficientInformation;
	private String followUpQuestion;
	private String department;
}
