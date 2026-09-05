package com.CampusCare.complaint;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface ComplaintRepository
	extends JpaRepository<Complaint, Long>, JpaSpecificationExecutor<Complaint> {

	List<Complaint> findByStudentIdOrderByCreatedAtDesc(Long studentId);

	List<Complaint> findByDepartmentIdOrderByCreatedAtDesc(Long departmentId);

	List<Complaint> findByDepartmentIdInOrderByCreatedAtDesc(List<Long> departmentIds);
}
