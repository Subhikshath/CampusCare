package com.CampusCare.complaint;

import com.CampusCare.ai.AIRequest;
import com.CampusCare.ai.AIResponse;
import com.CampusCare.ai.AIService;
import com.CampusCare.department.Department;
import com.CampusCare.department.DepartmentRepository;
import com.CampusCare.user.User;
import com.CampusCare.user.UserRepository;
import com.CampusCare.user.UserRole;
import com.CampusCare.storage.StorageService;
import com.CampusCare.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ComplaintService {

	private final ComplaintRepository complaintRepository;
	private final UserRepository userRepository;
	private final DepartmentRepository departmentRepository;
	private final AIService aiService;
	private final ComplaintHistoryRepository historyRepository;
	private final ComplaintConversationRepository conversationRepository;
	private final ComplaintImageRepository imageRepository;
	private final StorageService storageService;
	private final NotificationService notificationService;

	@Transactional
	public ComplaintResponse create(String studentEmail, ComplaintRequest request) {
		User student = userRepository.findByCollegeEmail(studentEmail)
				.orElseThrow(() -> new IllegalArgumentException("Student not found"));
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
		return toResponse(saved);
	}

	    @Transactional(readOnly = true)
	    public Page<ComplaintResponse> findStudentComplaints(String email,
								  ComplaintStatus status,
								  Long departmentId,
								  String search,
								  LocalDate fromDate,
								  LocalDate toDate,
								  Pageable pageable) {
		User student = userRepository.findByCollegeEmail(email)
			.orElseThrow(() -> new IllegalArgumentException("Student not found"));
		validateStudentEligibility(student);
		Specification<Complaint> specification = (root, query, builder) ->
			builder.equal(root.get("student").get("id"), student.getId());
		if (status != null) {
		    specification = specification.and((root, query, builder) ->
			    builder.equal(root.get("status"), status));
		}
		if (departmentId != null) {
		    specification = specification.and((root, query, builder) ->
			    builder.equal(root.get("department").get("id"), departmentId));
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
		return complaintRepository.findAll(specification, pageable).map(this::toResponse);
	    }

	    @Transactional(readOnly = true)
	    public ComplaintResponse findStudentComplaint(String email, Long complaintId) {
		Complaint complaint = complaintRepository.findById(complaintId)
			.orElseThrow(() -> new IllegalArgumentException("Complaint not found"));
		verifyStudent(email, complaint);
		return toResponse(complaint);
	    }

	    @Transactional(readOnly = true)
	    public List<ComplaintHistory> findHistory(String email, Long complaintId) {
		Complaint complaint = complaintRepository.findById(complaintId)
			.orElseThrow(() -> new IllegalArgumentException("Complaint not found"));
		verifyStudent(email, complaint);
		return historyRepository.findByComplaintIdOrderByCreatedAtAsc(complaintId);
	    }

	@Transactional
	public String addImage(String email, Long complaintId, MultipartFile file) {
		Complaint complaint = getComplaint(complaintId);
		verifyStudent(email, complaint);
		if (file.isEmpty() || file.getContentType() == null
				|| !file.getContentType().startsWith("image/")) {
			throw new IllegalArgumentException("Only non-empty image files are allowed");
		}
		String path = complaintId + "/" + System.currentTimeMillis() + "-" + file.getOriginalFilename();
		String url = storageService.upload(file, path);
		imageRepository.save(ComplaintImage.builder()
				.complaint(complaint)
				.imageUrl(url)
				.build());
		return url;
	}

	    @Transactional
	    public ComplaintResponse answerFollowUp(String email, Long complaintId,
						     FollowUpRequest request) {
		Complaint complaint = getComplaint(complaintId);
		verifyStudent(email, complaint);
		String information = request.getAdditionalInformation().trim();
		complaint.setAdditionalInformation(information);

		conversationRepository.save(ComplaintConversation.builder()
			.complaint(complaint)
			.senderType(ComplaintConversation.SenderType.STUDENT)
			.message(information)
			.build());

		AIRequest aiRequest = new AIRequest();
		aiRequest.setTitle(complaint.getTitle());
		aiRequest.setDescription(complaint.getDescription());
		aiRequest.setAdditionalInformation(information);
		AIResponse analysis = aiService.analyze(aiRequest);

		complaint.setAiFollowUpQuestion(analysis.getFollowUpQuestion());
		if (analysis.isSufficientInformation()
			&& analysis.getDepartment() != null
			&& !analysis.getDepartment().isBlank()) {
		    Department department = departmentRepository.findByCode(
				    analysis.getDepartment().trim().toUpperCase())
			    .orElseThrow(() -> new IllegalArgumentException(
				    "AI returned an unknown department"));
		    complaint.setDepartment(department);
		    complaint.setStatus(ComplaintStatus.DEPARTMENT_ASSIGNED);
		    complaint.setAiFollowUpQuestion(null);
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

	    private void verifyStudent(String email, Complaint complaint) {
		if (!complaint.getStudent().getCollegeEmail().equals(email)) {
		    throw new IllegalArgumentException("Unauthorized complaint access");
		}
	    }

	private AIRequest toAIRequest(ComplaintRequest request) {
		AIRequest aiRequest = new AIRequest();
		aiRequest.setTitle(request.getTitle());
		aiRequest.setDescription(request.getDescription());
		aiRequest.setAdditionalInformation(request.getAdditionalInformation());
		return aiRequest;
	}

	private ComplaintResponse toResponse(Complaint complaint) {
		return ComplaintResponse.builder()
				.id(complaint.getId())
				.title(complaint.getTitle())
				.description(complaint.getDescription())
				.additionalInformation(complaint.getAdditionalInformation())
				.studentId(complaint.getStudent().getId())
				.departmentId(complaint.getDepartment() == null
						? null : complaint.getDepartment().getId())
				.assignedWorkerId(complaint.getAssignedWorker() == null
					? null : complaint.getAssignedWorker().getId())
				.status(complaint.getStatus())
				.followUpQuestion(complaint.getAiFollowUpQuestion())
				.createdAt(complaint.getCreatedAt())
				.updatedAt(complaint.getUpdatedAt())
				.build();
	}

	@Transactional(readOnly = true)
	public List<ComplaintResponse> findDepartmentComplaints(String adminEmail) {
		User admin = getUser(adminEmail);
		List<Long> departmentIds = admin.getDepartments().stream()
				.map(com.CampusCare.department.Department::getId)
				.toList();
		return complaintRepository.findByDepartmentIdInOrderByCreatedAtDesc(departmentIds)
				.stream().map(this::toResponse).toList();
	}

	@Transactional
	public ComplaintResponse assignWorker(String adminEmail, Long complaintId, Long workerId) {
		Complaint complaint = getComplaint(complaintId);
		User admin = getUser(adminEmail);
		verifyDepartmentAccess(admin, complaint);
		User worker = userRepository.findById(workerId)
				.orElseThrow(() -> new IllegalArgumentException("Worker not found"));
		if (worker.getRole() != UserRole.WORKER
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
		return toResponse(saved);
	}

	@Transactional
	public ComplaintResponse updateStatus(String adminEmail, Long complaintId,
										   ComplaintStatus newStatus) {
		Complaint complaint = getComplaint(complaintId);
		User admin = getUser(adminEmail);
		verifyDepartmentAccess(admin, complaint);
		if (!isValidTransition(complaint.getStatus(), newStatus)) {
			throw new IllegalArgumentException("Invalid status transition");
		}
		ComplaintStatus oldStatus = complaint.getStatus();
		complaint.setStatus(newStatus);
		Complaint saved = complaintRepository.save(complaint);
		saveHistory(saved, oldStatus, newStatus, admin, "Complaint status updated");
		notificationService.create(saved.getStudent(), saved,
			"Complaint status changed to " + newStatus.name());
		return toResponse(saved);
	}

	private User getUser(String email) {
		return userRepository.findByCollegeEmail(email)
				.orElseThrow(() -> new IllegalArgumentException("User not found"));
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
				.orElseThrow(() -> new IllegalArgumentException("Complaint not found"));
	}

	private void verifyDepartmentAccess(User admin, Complaint complaint) {
		if (complaint.getDepartment() == null || admin.getDepartments().stream().noneMatch(
				department -> department.getId().equals(complaint.getDepartment().getId()))) {
			throw new IllegalArgumentException("Unauthorized department access");
		}
	}

	private boolean isValidTransition(ComplaintStatus current, ComplaintStatus next) {
		if (current == next) {
			return true;
		}
		return switch (current) {
			case DEPARTMENT_ASSIGNED -> next == ComplaintStatus.PENDING_DEPARTMENT_REVIEW
					|| next == ComplaintStatus.REJECTED;
			case PENDING_DEPARTMENT_REVIEW -> next == ComplaintStatus.WORKER_ASSIGNED
					|| next == ComplaintStatus.REJECTED;
			case WORKER_ASSIGNED -> next == ComplaintStatus.IN_PROGRESS;
			case IN_PROGRESS -> next == ComplaintStatus.RESOLVED;
			case RESOLVED -> next == ComplaintStatus.CLOSED;
			default -> false;
		};
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
}
