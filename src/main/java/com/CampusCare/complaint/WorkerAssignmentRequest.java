package com.CampusCare.complaint;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WorkerAssignmentRequest {
    @NotNull(message = "Worker ID is required")
    private Long workerId;
}