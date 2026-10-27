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
	String departmentName;
	Long assignedWorkerId;
	boolean workerAssigned;
	ComplaintStatus status;
	String followUpQuestion;
	LocalDateTime createdAt;
	LocalDateTime updatedAt;
	LocalDateTime resolvedAt;
}
