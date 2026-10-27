package com.CampusCare.complaint;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface ComplaintRepository
	extends JpaRepository<Complaint, Long>, JpaSpecificationExecutor<Complaint> {

	List<Complaint> findByStudentIdOrderByCreatedAtDesc(Long studentId);

	List<Complaint> findByDepartmentIdOrderByCreatedAtDesc(Long departmentId);

	List<Complaint> findByDepartmentIdInOrderByCreatedAtDesc(List<Long> departmentIds);

	List<Complaint> findByAssignedWorkerIdOrderByCreatedAtDesc(Long workerId);

	long countByStatus(ComplaintStatus status);

	@org.springframework.data.jpa.repository.Query("select c.department.name, count(c) from Complaint c where c.department is not null group by c.department.name")
	List<Object[]> countByDepartment();
}
