package com.CampusCare.notification;

import com.CampusCare.complaint.Complaint;
import com.CampusCare.user.User;
import com.CampusCare.user.UserRepository;
import com.CampusCare.user.UserRole;
import com.CampusCare.common.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

	private final NotificationRepository notificationRepository;
	private final UserRepository userRepository;

	@Transactional
	public void create(User user, Complaint complaint, String message) {
		notificationRepository.save(Notification.builder()
				.user(user)
				.complaint(complaint)
				.message(message)
				.build());
	}

	@Transactional
	public void notifyDepartmentAdmins(Complaint complaint, String message) {
		if (complaint.getDepartment() == null) {
			return;
		}
		userRepository.findByRole(UserRole.DEPARTMENT_ADMIN).stream()
				.filter(admin -> admin.getDepartments().stream()
						.anyMatch(department -> department.getId().equals(complaint.getDepartment().getId())))
				.forEach(admin -> create(admin, complaint, message));
	}

	@Transactional(readOnly = true)
	public List<NotificationResponse> findMine(String email) {
		Long userId = userRepository.findByCollegeEmail(email)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"))
				.getId();
		return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional
	public void markRead(String email, Long id) {
		Notification notification = notificationRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
		if (!notification.getUser().getCollegeEmail().equals(email)) {
			throw new AccessDeniedException("Unauthorized notification access");
		}
		notification.setRead(true);
	}

	private NotificationResponse toResponse(Notification notification) {
		return NotificationResponse.builder()
				.id(notification.getId())
				.message(notification.getMessage())
				.read(notification.isRead())
				.complaintId(notification.getComplaint() == null
						? null : notification.getComplaint().getId())
				.createdAt(notification.getCreatedAt())
				.build();
	}
}
