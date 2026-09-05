package com.CampusCare.complaint;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StatusUpdateRequest {
    @NotNull(message = "Status is required")
    private ComplaintStatus status;
}