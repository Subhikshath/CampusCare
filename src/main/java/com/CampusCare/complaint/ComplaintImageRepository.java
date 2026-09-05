package com.CampusCare.complaint;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComplaintImageRepository extends JpaRepository<ComplaintImage, Long> {
	List<ComplaintImage> findByComplaintIdOrderByCreatedAtAsc(Long complaintId);
}
