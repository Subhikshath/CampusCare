package com.CampusCare.ai;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AIRequest {

	@NotBlank
	private String title;

	@NotBlank
	private String description;

	private String additionalInformation;
}
