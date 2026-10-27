package com.CampusCare.notification;

import com.CampusCare.complaint.Complaint;
import com.CampusCare.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String message;

	@Builder.Default
	@Column(name = "is_read", nullable = false)
	private boolean read = false;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "complaint_id")
	private Complaint complaint;

	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@PrePersist
	public void onCreate() {
		createdAt = LocalDateTime.now();
	}
}
