package com.CampusCare.complaint;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class ComplaintResponse {

	Long id;
	String title;
	String description;
	String additionalInformation;
	Long studentId;
	Long departmentId;
	Long assignedWorkerId;
	ComplaintStatus status;
	String followUpQuestion;
	LocalDateTime createdAt;
	LocalDateTime updatedAt;
}
