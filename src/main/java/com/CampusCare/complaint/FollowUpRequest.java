package com.CampusCare.complaint;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FollowUpRequest {
    @NotBlank(message = "Additional information is required")
    private String additionalInformation;
}