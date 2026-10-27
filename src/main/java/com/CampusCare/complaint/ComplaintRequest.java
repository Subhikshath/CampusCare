package com.CampusCare.complaint;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ComplaintRequest {

	@NotBlank(message = "Title is required")
	@Size(max = 200, message = "Title must not exceed 200 characters")
	private String title;

	@NotBlank(message = "Description is required")
	@Size(max = 10000, message = "Description must not exceed 10000 characters")
	private String description;

	@Size(max = 10000, message = "Additional information must not exceed 10000 characters")
	private String additionalInformation;
}
