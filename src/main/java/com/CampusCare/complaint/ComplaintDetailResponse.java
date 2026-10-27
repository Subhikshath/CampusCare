package com.CampusCare.complaint;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;

@Value
@Builder
public class ComplaintDetailResponse {
	Long id;
	String title;
	String description;
	String additionalInformation;
	Long studentId;
	String studentName;
	String studentEmail;
	String studentRollNo;
	Long departmentId;
	String departmentName;
	Long assignedWorkerId;
	String assignedWorkerName;
	String assignedWorkerEmail;
	boolean workerAssigned;
	ComplaintStatus status;
	String followUpQuestion;
	LocalDateTime createdAt;
	LocalDateTime updatedAt;
	LocalDateTime resolvedAt;
	List<ComplaintConversationResponse> conversation;
	List<ComplaintHistoryResponse> history;
}
