package com.CampusCare.complaint;

import com.CampusCare.ai.AIRequest;
import com.CampusCare.ai.AIResponse;
import com.CampusCare.ai.AIService;
import com.CampusCare.department.Department;
import com.CampusCare.department.DepartmentRepository;
import com.CampusCare.user.User;
import com.CampusCare.user.UserRepository;
import com.CampusCare.user.UserRole;
import com.CampusCare.notification.NotificationService;
import com.CampusCare.common.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ComplaintService {

	private final ComplaintRepository complaintRepository;
	private final UserRepository userRepository;
	private final DepartmentRepository departmentRepository;
	private final AIService aiService;
	private final ComplaintHistoryRepository historyRepository;
	private final ComplaintConversationRepository conversationRepository;
	private final NotificationService notificationService;

	@Transactional
	public ComplaintResponse create(String studentEmail, ComplaintRequest request) {
		User student = userRepository.findByCollegeEmail(studentEmail)
				.orElseThrow(() -> new ResourceNotFoundException("Student not found"));
		validateStudentEligibility(student);

		AIResponse analysis = aiService.analyze(toAIRequest(request));
		Complaint complaint = Complaint.builder()
				.title(request.getTitle().trim())
				.description(request.getDescription().trim())
				.additionalInformation(request.getAdditionalInformation())
				.student(student)
				.status(ComplaintStatus.AWAITING_INFORMATION)
				.aiFollowUpQuestion(analysis.getFollowUpQuestion())
				.build();

		if (analysis.isSufficientInformation()
			&& analysis.getDepartment() != null
			&& !analysis.getDepartment().isBlank()) {
			Department department = departmentRepository.findByCode(
							analysis.getDepartment().trim().toUpperCase())
					.orElseThrow(() -> new IllegalArgumentException(
							"AI returned an unknown department"));
			if (!department.isEnabled()) {
				throw new IllegalArgumentException("Assigned department is disabled");
			}
			complaint.setDepartment(department);
			complaint.setStatus(ComplaintStatus.DEPARTMENT_ASSIGNED);
			complaint.setAiFollowUpQuestion(null);
		} else if (analysis.isSufficientInformation()) {
			throw new IllegalArgumentException("AI returned no department");
		}

		Complaint saved = complaintRepository.save(complaint);
		conversationRepository.save(ComplaintConversation.builder()
			.complaint(saved)
			.senderType(ComplaintConversation.SenderType.STUDENT)
			.message(request.getDescription())
			.build());
		if (request.getAdditionalInformation() != null
				&& !request.getAdditionalInformation().isBlank()) {
			conversationRepository.save(ComplaintConversation.builder()
				.complaint(saved)
				.senderType(ComplaintConversation.SenderType.STUDENT)
				.message(request.getAdditionalInformation().trim())
				.build());
		}
		if (analysis.getFollowUpQuestion() != null) {
		    conversationRepository.save(ComplaintConversation.builder()
			    .complaint(saved)
			    .senderType(ComplaintConversation.SenderType.AI)
			    .message(analysis.getFollowUpQuestion())
			    .build());
		}
		historyRepository.save(ComplaintHistory.builder()
			.complaint(saved)
			.action("COMPLAINT_CREATED")
			.newStatus(saved.getStatus())
			.performedBy(student)
			.description("Complaint submitted by student")
			.build());
		notificationService.create(student, saved,
			saved.getStatus() == ComplaintStatus.AWAITING_INFORMATION
				? "AI requested additional complaint information"
				: "Complaint assigned to a department");
		if (saved.getDepartment() != null) {
			notificationService.notifyDepartmentAdmins(saved, "A new complaint was assigned to your department");
		}
		return toResponse(saved);
	}

	    @Transactional(readOnly = true)
	    public Page<ComplaintResponse> findStudentComplaints(String email,
								  ComplaintStatus status,
								  Long departmentId,
								  String search,
								  LocalDate fromDate,
								  LocalDate toDate,
								  String group,
								  Boolean workerAssigned,
								  Pageable pageable) {
		User student = userRepository.findByCollegeEmail(email)
			.orElseThrow(() -> new ResourceNotFoundException("Student not found"));
		validateStudentEligibility(student);
		Specification<Complaint> specification = (root, query, builder) ->
			builder.equal(root.get("student").get("id"), student.getId());
		if (status != null) {
		    specification = specification.and((root, query, builder) ->
			    builder.equal(root.get("status"), status));
		}
		if (group != null && !group.isBlank()) {
		    List<ComplaintStatus> groupedStatuses = switch (group.toLowerCase()) {
			    case "progress" -> List.of(
					    ComplaintStatus.AWAITING_INFORMATION,
					    ComplaintStatus.AI_ANALYZING,
					    ComplaintStatus.DEPARTMENT_ASSIGNED,
					    ComplaintStatus.PENDING_DEPARTMENT_REVIEW,
					    ComplaintStatus.WORKER_ASSIGNED,
					    ComplaintStatus.IN_PROGRESS,
					    ComplaintStatus.ON_HOLD,
					    ComplaintStatus.WAITING_FOR_PARTS,
					    ComplaintStatus.WAITING_FOR_ACCESS);
			    case "completed" -> List.of(
					    ComplaintStatus.RESOLVED, ComplaintStatus.CLOSED);
			    case "all" -> List.of();
			    default -> throw new IllegalArgumentException(
					    "Group must be all, progress, or completed");
		    };
		    if (!groupedStatuses.isEmpty()) {
			    specification = specification.and((root, query, builder) ->
					    root.get("status").in(groupedStatuses));
		    }
		}
		if (departmentId != null) {
		    specification = specification.and((root, query, builder) ->
			    builder.equal(root.get("department").get("id"), departmentId));
		}
		if (workerAssigned != null) {
		    specification = specification.and((root, query, builder) ->
			    workerAssigned
					    ? builder.isNotNull(root.get("assignedWorker"))
					    : builder.isNull(root.get("assignedWorker")));
		}
		if (search != null && !search.isBlank()) {
		    String pattern = "%" + search.toLowerCase() + "%";
		    specification = specification.and((root, query, builder) -> builder.or(
			    builder.like(builder.lower(root.get("title")), pattern),
			    builder.like(builder.lower(root.get("description")), pattern)));
		}
		if (fromDate != null) {
		    LocalDateTime start = fromDate.atStartOfDay();
		    specification = specification.and((root, query, builder) ->
			    builder.greaterThanOrEqualTo(root.get("createdAt"), start));
		}
		if (toDate != null) {
		    LocalDateTime end = toDate.plusDays(1).atStartOfDay();
		    specification = specification.and((root, query, builder) ->
			    builder.lessThan(root.get("createdAt"), end));
		}
		return complaintRepository.findAll(specification, pageable)
				.map(complaint -> toResponse(complaint, false));
	    }

	    @Transactional(readOnly = true)
	    public ComplaintResponse findStudentComplaint(String email, Long complaintId) {
		Complaint complaint = getComplaint(complaintId);
		verifyStudent(email, complaint);
		return toResponse(complaint);
	    }

	    @Transactional(readOnly = true)
	    public ComplaintDetailResponse findStudentComplaintDetails(String email, Long complaintId) {
		Complaint complaint = getComplaint(complaintId);
		verifyStudent(email, complaint);
		return toDetailResponse(complaint, false);
	    }

	    @Transactional(readOnly = true)
		public List<ComplaintHistoryResponse> findHistory(String email, Long complaintId) {
		Complaint complaint = getComplaint(complaintId);
		verifyStudent(email, complaint);
		return historyRepository.findByComplaintIdOrderByCreatedAtAsc(complaintId).stream()
				.map(this::toHistoryResponse).toList();
	    }

	    @Transactional
	    public ComplaintResponse answerFollowUp(String email, Long complaintId,
						     FollowUpRequest request) {
		Complaint complaint = getComplaint(complaintId);
		verifyStudent(email, complaint);
		if (complaint.getStatus() != ComplaintStatus.AWAITING_INFORMATION
				|| complaint.getAiFollowUpQuestion() == null) {
			throw new IllegalArgumentException("This complaint is not waiting for more information");
		}
		String information = request.getAdditionalInformation().trim();
		complaint.setAdditionalInformation(appendInformation(
				complaint.getAdditionalInformation(), information));

		String conversationContext = conversationRepository
				.findByComplaintIdOrderByCreatedAtAsc(complaintId).stream()
				.map(message -> message.getSenderType() + ": " + message.getMessage())
				.collect(Collectors.joining("\n"));
		conversationContext = conversationContext.isBlank()
				? "STUDENT: " + information
				: conversationContext + "\nSTUDENT: " + information;

		conversationRepository.save(ComplaintConversation.builder()
			.complaint(complaint)
			.senderType(ComplaintConversation.SenderType.STUDENT)
			.message(information)
			.build());

		AIRequest aiRequest = new AIRequest();
		aiRequest.setTitle(complaint.getTitle());
		aiRequest.setDescription(complaint.getDescription());
		aiRequest.setAdditionalInformation(conversationContext);
		AIResponse analysis = aiService.analyze(aiRequest);

		complaint.setAiFollowUpQuestion(analysis.getFollowUpQuestion());
		if (analysis.isSufficientInformation()
			&& analysis.getDepartment() != null
			&& !analysis.getDepartment().isBlank()) {
		    Department department = departmentRepository.findByCode(
				    analysis.getDepartment().trim().toUpperCase())
			    .orElseThrow(() -> new IllegalArgumentException(
				    "AI returned an unknown department"));
			if (!department.isEnabled()) {
				throw new IllegalArgumentException("Assigned department is disabled");
			}
		    complaint.setDepartment(department);
		    complaint.setStatus(ComplaintStatus.DEPARTMENT_ASSIGNED);
		    complaint.setAiFollowUpQuestion(null);
			notificationService.notifyDepartmentAdmins(complaint,
				"A complaint was assigned to your department after follow-up information");
		} else {
			complaint.setStatus(ComplaintStatus.AWAITING_INFORMATION);
		}

		Complaint saved = complaintRepository.save(complaint);
		if (analysis.getFollowUpQuestion() != null) {
		    conversationRepository.save(ComplaintConversation.builder()
			    .complaint(saved)
			    .senderType(ComplaintConversation.SenderType.AI)
			    .message(analysis.getFollowUpQuestion())
			    .build());
		}
		historyRepository.save(ComplaintHistory.builder()
			.complaint(saved)
			.action("FOLLOW_UP_PROVIDED")
			.newStatus(saved.getStatus())
			.performedBy(saved.getStudent())
			.description("Student provided additional complaint information")
			.build());
		return toResponse(saved);
	    }

	@Transactional(readOnly = true)
	public List<ComplaintResponse> findAllForAdmin() {
		return complaintRepository.findAll(org.springframework.data.domain.Sort.by(
				org.springframework.data.domain.Sort.Direction.DESC, "createdAt"))
				.stream().map(this::toResponse).toList();
	}

	    private void verifyStudent(String email, Complaint complaint) {
		if (!complaint.getStudent().getCollegeEmail().equals(email)) {
				throw new AccessDeniedException("Unauthorized complaint access");
		}
	    }

	private AIRequest toAIRequest(ComplaintRequest request) {
		AIRequest aiRequest = new AIRequest();
		aiRequest.setTitle(request.getTitle());
		aiRequest.setDescription(request.getDescription());
		aiRequest.setAdditionalInformation(request.getAdditionalInformation());
		return aiRequest;
	}

	private String appendInformation(String existing, String additional) {
		if (existing == null || existing.isBlank()) {
			return additional;
		}
		return existing + "\n" + additional;
	}

	private ComplaintResponse toResponse(Complaint complaint) {
		return toResponse(complaint, true);
	}

	private ComplaintResponse toResponse(Complaint complaint, boolean includeWorkerIdentity) {
		return ComplaintResponse.builder()
				.id(complaint.getId())
				.title(complaint.getTitle())
				.description(complaint.getDescription())
				.additionalInformation(complaint.getAdditionalInformation())
				.studentId(complaint.getStudent().getId())
				.departmentId(complaint.getDepartment() == null
						? null : complaint.getDepartment().getId())
				.departmentName(complaint.getDepartment() == null
						? null : complaint.getDepartment().getName())
				.assignedWorkerId(!includeWorkerIdentity || complaint.getAssignedWorker() == null
					? null : complaint.getAssignedWorker().getId())
				.workerAssigned(complaint.getAssignedWorker() != null)
				.status(complaint.getStatus())
				.followUpQuestion(complaint.getAiFollowUpQuestion())
				.createdAt(complaint.getCreatedAt())
				.updatedAt(complaint.getUpdatedAt())
				.resolvedAt(complaint.getResolvedAt())
				.build();
	}

	@Transactional(readOnly = true)
	public List<ComplaintResponse> findDepartmentComplaints(String adminEmail) {
		User admin = getUser(adminEmail);
		if (admin.getRole() != UserRole.DEPARTMENT_ADMIN) {
			throw new IllegalArgumentException("Only department admins can access this resource");
		}
		List<Long> departmentIds = admin.getDepartments().stream()
				.map(com.CampusCare.department.Department::getId)
				.toList();
		return complaintRepository.findByDepartmentIdInOrderByCreatedAtDesc(departmentIds)
				.stream().map(this::toResponse).toList();
	}

	@Transactional(readOnly = true)
	public ComplaintResponse findDepartmentComplaint(String adminEmail, Long complaintId) {
		Complaint complaint = getComplaint(complaintId);
		verifyDepartmentAccess(getUser(adminEmail), complaint);
		return toResponse(complaint);
	}

	@Transactional(readOnly = true)
	public ComplaintDetailResponse findDepartmentComplaintDetails(
			String adminEmail, Long complaintId) {
		Complaint complaint = getComplaint(complaintId);
		verifyDepartmentAccess(getUser(adminEmail), complaint);
		return toDetailResponse(complaint, true);
	}

	@Transactional(readOnly = true)
	public ComplaintDetailResponse findAdminComplaintDetails(String adminEmail, Long complaintId) {
		User admin = getUser(adminEmail);
		if (admin.getRole() != UserRole.ADMIN) {
			throw new AccessDeniedException("Only administrators can view all complaint details");
		}
		return toDetailResponse(getComplaint(complaintId), true);
	}

	@Transactional(readOnly = true)
	public List<ComplaintHistoryResponse> findDepartmentHistory(String adminEmail, Long complaintId) {
		Complaint complaint = getComplaint(complaintId);
		verifyDepartmentAccess(getUser(adminEmail), complaint);
		return historyRepository.findByComplaintIdOrderByCreatedAtAsc(complaintId).stream()
				.map(this::toHistoryResponse).toList();
	}

	@Transactional(readOnly = true)
	public List<ComplaintResponse> findWorkerComplaints(String workerEmail) {
		User worker = getUser(workerEmail);
		if (worker.getRole() != UserRole.WORKER) {
			throw new IllegalArgumentException("Only workers can access this resource");
		}
		return complaintRepository.findByAssignedWorkerIdOrderByCreatedAtDesc(worker.getId())
				.stream().map(this::toResponse).toList();
	}

	@Transactional(readOnly = true)
	public ComplaintResponse findWorkerComplaint(String workerEmail, Long complaintId) {
		Complaint complaint = getComplaint(complaintId);
		User worker = getUser(workerEmail);
		if (complaint.getAssignedWorker() == null
				|| !complaint.getAssignedWorker().getId().equals(worker.getId())) {
			throw new AccessDeniedException("Unauthorized complaint access");
		}
		return toResponse(complaint);
	}

	@Transactional
	public ComplaintResponse assignWorker(String adminEmail, Long complaintId, Long workerId) {
		Complaint complaint = getComplaint(complaintId);
		User admin = getUser(adminEmail);
		verifyDepartmentAccess(admin, complaint);
		if (complaint.getStatus() != ComplaintStatus.DEPARTMENT_ASSIGNED
				&& complaint.getStatus() != ComplaintStatus.PENDING_DEPARTMENT_REVIEW) {
			throw new IllegalArgumentException(
					"Complaint must be assigned to the department before worker assignment");
		}
		User worker = userRepository.findById(workerId)
				.orElseThrow(() -> new ResourceNotFoundException("Worker not found"));
		if (worker.getRole() != UserRole.WORKER
				|| !worker.isEnabled()
				|| complaint.getDepartment() == null
				|| worker.getDepartments().stream().noneMatch(department ->
				department.getId().equals(complaint.getDepartment().getId()))) {
			throw new IllegalArgumentException("Worker does not belong to complaint department");
		}
		complaint.setAssignedWorker(worker);
		ComplaintStatus oldStatus = complaint.getStatus();
		complaint.setStatus(ComplaintStatus.WORKER_ASSIGNED);
		Complaint saved = complaintRepository.save(complaint);
		saveHistory(saved, oldStatus, saved.getStatus(), admin, "Worker assigned");
		notificationService.create(worker, saved, "You have been assigned a complaint");
		notificationService.create(saved.getStudent(), saved,
				"Your report is now worker assigned");
		notificationService.notifyDepartmentAdmins(saved,
				"A worker was assigned to a complaint in your department");
		return toResponse(saved);
	}

	@Transactional
	public ComplaintResponse updateStatus(String adminEmail, Long complaintId,
										   ComplaintStatus newStatus) {
		Complaint complaint = getComplaint(complaintId);
		User admin = getUser(adminEmail);
		if (admin.getRole() == UserRole.DEPARTMENT_ADMIN) {
			verifyDepartmentAccess(admin, complaint);
		} else if (admin.getRole() != UserRole.ADMIN) {
			throw new AccessDeniedException("Only administrators can change complaint status");
		}
		if (!isValidTransition(complaint.getStatus(), newStatus)) {
			throw new IllegalArgumentException("Invalid status transition");
		}
		ComplaintStatus oldStatus = complaint.getStatus();
		complaint.setStatus(newStatus);
		if (newStatus == ComplaintStatus.RESOLVED) {
			complaint.setResolvedAt(LocalDateTime.now());
		}
		Complaint saved = complaintRepository.save(complaint);
		saveHistory(saved, oldStatus, newStatus, admin, "Complaint status updated");
		notificationService.create(saved.getStudent(), saved,
			"Your report is now " + displayStatus(newStatus));
		notificationService.notifyDepartmentAdmins(saved,
				"Complaint status changed to " + displayStatus(newStatus));
		return toResponse(saved);
	}

	private String displayStatus(ComplaintStatus status) {
		return status == ComplaintStatus.RESOLVED
				? "completed"
				: status.name().toLowerCase().replace('_', ' ');
	}

	private User getUser(String email) {
		return userRepository.findByCollegeEmail(email)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));
	}

	private void validateStudentEligibility(User student) {
		if (student.getRole() != UserRole.STUDENT || student.getRollNo() == null
				|| student.getRollNo().isBlank()) {
			throw new IllegalArgumentException(
					"Only students with a roll number can submit complaints");
		}
	}

	private Complaint getComplaint(Long id) {
		return complaintRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Complaint not found"));
	}

	private void verifyDepartmentAccess(User admin, Complaint complaint) {
		if (complaint.getDepartment() == null || admin.getDepartments().stream().noneMatch(
				department -> department.getId().equals(complaint.getDepartment().getId()))) {
			throw new AccessDeniedException("Unauthorized department access");
		}
	}

	private boolean isValidTransition(ComplaintStatus current, ComplaintStatus next) {
		if (current == next) {
			return false;
		}
		return switch (current) {
			case AWAITING_INFORMATION -> next == ComplaintStatus.AI_ANALYZING;
			case AI_ANALYZING -> next == ComplaintStatus.AWAITING_INFORMATION
					|| next == ComplaintStatus.DEPARTMENT_ASSIGNED;
			case DEPARTMENT_ASSIGNED -> next == ComplaintStatus.PENDING_DEPARTMENT_REVIEW
					|| next == ComplaintStatus.REJECTED;
			case PENDING_DEPARTMENT_REVIEW -> next == ComplaintStatus.WORKER_ASSIGNED
					|| next == ComplaintStatus.REJECTED;
			case WORKER_ASSIGNED -> next == ComplaintStatus.IN_PROGRESS;
			case IN_PROGRESS -> next == ComplaintStatus.ON_HOLD
					|| next == ComplaintStatus.WAITING_FOR_PARTS
					|| next == ComplaintStatus.WAITING_FOR_ACCESS
					|| next == ComplaintStatus.RESOLVED;
			case ON_HOLD, WAITING_FOR_PARTS, WAITING_FOR_ACCESS ->
					next == ComplaintStatus.IN_PROGRESS;
			case RESOLVED -> next == ComplaintStatus.CLOSED;
			default -> false;
		};
	}

	private ComplaintDetailResponse toDetailResponse(
			Complaint complaint, boolean includeWorkerIdentity) {
		User worker = complaint.getAssignedWorker();
		User student = complaint.getStudent();
		return ComplaintDetailResponse.builder()
				.id(complaint.getId())
				.title(complaint.getTitle())
				.description(complaint.getDescription())
				.additionalInformation(complaint.getAdditionalInformation())
				.studentId(student.getId())
				.studentName(student.getName())
				.studentEmail(student.getCollegeEmail())
				.studentRollNo(student.getRollNo())
				.departmentId(complaint.getDepartment() == null
						? null : complaint.getDepartment().getId())
				.departmentName(complaint.getDepartment() == null
						? null : complaint.getDepartment().getName())
				.assignedWorkerId(!includeWorkerIdentity || worker == null ? null : worker.getId())
				.assignedWorkerName(!includeWorkerIdentity || worker == null ? null : worker.getName())
				.assignedWorkerEmail(!includeWorkerIdentity || worker == null
						? null : worker.getCollegeEmail())
				.workerAssigned(worker != null)
				.status(complaint.getStatus())
				.followUpQuestion(complaint.getAiFollowUpQuestion())
				.createdAt(complaint.getCreatedAt())
				.updatedAt(complaint.getUpdatedAt())
				.resolvedAt(complaint.getResolvedAt())
				.conversation(conversationRepository
						.findByComplaintIdOrderByCreatedAtAsc(complaint.getId()).stream()
						.map(ComplaintConversationResponse::from)
						.toList())
				.history(historyRepository.findByComplaintIdOrderByCreatedAtAsc(
						complaint.getId()).stream().map(this::toHistoryResponse).toList())
				.build();
	}

	private void saveHistory(Complaint complaint, ComplaintStatus oldStatus,
							 ComplaintStatus newStatus, User admin, String description) {
		historyRepository.save(ComplaintHistory.builder()
				.complaint(complaint)
				.action("STATUS_CHANGED")
				.oldStatus(oldStatus)
				.newStatus(newStatus)
				.performedBy(admin)
				.description(description)
				.build());
	}

	private ComplaintHistoryResponse toHistoryResponse(ComplaintHistory history) {
		User performedBy = history.getPerformedBy();
		return ComplaintHistoryResponse.builder()
				.id(history.getId())
				.action(history.getAction())
				.oldStatus(history.getOldStatus())
				.newStatus(history.getNewStatus())
				.performedById(performedBy == null ? null : performedBy.getId())
				.performedByName(performedBy == null ? null : performedBy.getName())
				.description(history.getDescription())
				.createdAt(history.getCreatedAt())
				.build();
	}
}
