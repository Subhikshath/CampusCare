package com.CampusCare.complaint;

import com.CampusCare.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "complaint_histories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplaintHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "complaint_id", nullable = false)
	private Complaint complaint;

	@Column(nullable = false, length = 100)
	private String action;

	@Enumerated(EnumType.STRING)
	private ComplaintStatus oldStatus;

	@Enumerated(EnumType.STRING)
	private ComplaintStatus newStatus;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "performed_by_id")
	private User performedBy;

	@Column(columnDefinition = "TEXT")
	private String description;

	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@PrePersist
	public void onCreate() {
		createdAt = LocalDateTime.now();
	}
}
