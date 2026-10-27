package com.CampusCare.complaint;

import com.CampusCare.department.Department;
import com.CampusCare.department.DepartmentRepository;
import com.CampusCare.user.User;
import com.CampusCare.user.UserRepository;
import com.CampusCare.user.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ComplaintWorkflowTest {

	@Autowired
	private ComplaintService complaintService;

	@Autowired
	private ComplaintRepository complaintRepository;

	@Autowired
	private DepartmentRepository departmentRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ComplaintConversationRepository conversationRepository;

	@Test
	void departmentAdminAssignsWorkAndMovesReportToCompletionVisibleInDetails() {
		TestData data = createTestData();
		Complaint complaint = complaintRepository.save(Complaint.builder()
				.title("Water leak in room 204")
				.description("Water is leaking in room 204.")
				.additionalInformation("Block A, floor 2.")
				.student(data.student())
				.department(data.department())
				.status(ComplaintStatus.DEPARTMENT_ASSIGNED)
				.build());
		complaintRepository.flush();
		conversationRepository.save(ComplaintConversation.builder()
				.complaint(complaint)
				.senderType(ComplaintConversation.SenderType.STUDENT)
				.message("Water is leaking in room 204.")
				.build());
		conversationRepository.save(ComplaintConversation.builder()
				.complaint(complaint)
				.senderType(ComplaintConversation.SenderType.AI)
				.message("The report has enough location information.")
				.build());
		complaintService.assignWorker(data.departmentAdmin().getCollegeEmail(),
				complaint.getId(), data.worker().getId());

		Complaint assigned = complaintRepository.findById(complaint.getId()).orElseThrow();
		assertEquals(ComplaintStatus.WORKER_ASSIGNED, assigned.getStatus());

		complaintService.updateStatus(data.departmentAdmin().getCollegeEmail(),
				complaint.getId(), ComplaintStatus.IN_PROGRESS);
		complaintService.updateStatus(data.departmentAdmin().getCollegeEmail(),
				complaint.getId(), ComplaintStatus.RESOLVED);

		Complaint completed = complaintRepository.findById(complaint.getId()).orElseThrow();
		assertEquals(ComplaintStatus.RESOLVED, completed.getStatus());
		assertNotNull(completed.getResolvedAt());

		ComplaintDetailResponse studentDetails = complaintService.findStudentComplaintDetails(
				data.student().getCollegeEmail(), complaint.getId());
		assertEquals(data.department().getName(), studentDetails.getDepartmentName());
		assertNull(studentDetails.getAssignedWorkerName());
		assertTrue(studentDetails.isWorkerAssigned());
		assertEquals("Block A, floor 2.", studentDetails.getAdditionalInformation());
		assertEquals(ComplaintStatus.RESOLVED, studentDetails.getStatus());
		assertEquals(List.of(
				ComplaintStatus.WORKER_ASSIGNED,
				ComplaintStatus.IN_PROGRESS,
				ComplaintStatus.RESOLVED),
				studentDetails.getHistory().stream()
						.map(ComplaintHistoryResponse::getNewStatus)
						.toList());

		ComplaintDetailResponse adminDetails = complaintService.findAdminComplaintDetails(
				data.mainAdmin().getCollegeEmail(), complaint.getId());
		assertEquals(data.student().getName(), adminDetails.getStudentName());
		assertEquals(data.student().getCollegeEmail(), adminDetails.getStudentEmail());
		assertEquals(data.worker().getCollegeEmail(), adminDetails.getAssignedWorkerEmail());
		assertTrue(adminDetails.getConversation().stream()
				.anyMatch(message -> message.message().contains("room 204")));
		assertEquals(3, adminDetails.getHistory().size());
	}

	@Test
	void studentComplaintFiltersCombineGroupAndWorkerAssignmentWithoutExposingWorkerIdentity() {
		TestData data = createTestData();
		Complaint assigned = complaintRepository.save(Complaint.builder()
				.title("Assigned report")
				.description("Repair in the north block.")
				.student(data.student())
				.department(data.department())
				.assignedWorker(data.worker())
				.status(ComplaintStatus.IN_PROGRESS)
				.build());
		complaintRepository.save(Complaint.builder()
				.title("Unassigned report")
				.description("Lighting issue in the library.")
				.student(data.student())
				.department(data.department())
				.status(ComplaintStatus.DEPARTMENT_ASSIGNED)
				.build());

		var assignedReports = complaintService.findStudentComplaints(
				data.student().getCollegeEmail(), null, null, null, null, null,
				"progress", true, PageRequest.of(0, 10));
		var unassignedReports = complaintService.findStudentComplaints(
				data.student().getCollegeEmail(), null, null, null, null, null,
				"progress", false, PageRequest.of(0, 10));

		assertEquals(1, assignedReports.getTotalElements());
		assertEquals(assigned.getId(), assignedReports.getContent().get(0).getId());
		assertTrue(assignedReports.getContent().get(0).isWorkerAssigned());
		assertEquals(1, unassignedReports.getTotalElements());
		assertEquals("Unassigned report", unassignedReports.getContent().get(0).getTitle());
	}

	@Test
	void mainAdminCanMoveWorkThroughAdditionalProgressStatuses() {
		TestData data = createTestData();
		Complaint complaint = complaintRepository.save(Complaint.builder()
				.title("Access issue")
				.description("The repair team needs access to the east wing.")
				.student(data.student())
				.department(data.department())
				.status(ComplaintStatus.IN_PROGRESS)
				.build());

		complaintService.updateStatus(data.mainAdmin().getCollegeEmail(),
				complaint.getId(), ComplaintStatus.WAITING_FOR_ACCESS);
		complaintService.updateStatus(data.mainAdmin().getCollegeEmail(),
				complaint.getId(), ComplaintStatus.IN_PROGRESS);

		assertEquals(ComplaintStatus.IN_PROGRESS,
				complaintRepository.findById(complaint.getId()).orElseThrow().getStatus());
	}

	@Test
	void departmentAdminCannotReadOrChangeReportsOutsideTheirDepartment() {
		TestData data = createTestData();
		User otherDepartmentAdmin = createUser(
				"Other department admin", "DEPARTMENT_ADMIN", null, List.of());
		Complaint complaint = complaintRepository.save(Complaint.builder()
				.title("Broken tap")
				.description("The tap is broken in the east wing.")
				.student(data.student())
				.department(data.department())
				.status(ComplaintStatus.DEPARTMENT_ASSIGNED)
				.build());

		assertThrows(AccessDeniedException.class, () ->
				complaintService.findDepartmentComplaintDetails(
						otherDepartmentAdmin.getCollegeEmail(), complaint.getId()));
		assertThrows(AccessDeniedException.class, () ->
				complaintService.updateStatus(otherDepartmentAdmin.getCollegeEmail(),
						complaint.getId(), ComplaintStatus.PENDING_DEPARTMENT_REVIEW));
	}

	@Test
	void adminDetailsEndpointServiceRejectsNonAdminUsers() {
		TestData data = createTestData();
		Complaint complaint = complaintRepository.save(Complaint.builder()
				.title("Damaged desk")
				.description("A desk is damaged in the library.")
				.student(data.student())
				.department(data.department())
				.status(ComplaintStatus.DEPARTMENT_ASSIGNED)
				.build());

		assertThrows(AccessDeniedException.class, () ->
				complaintService.findAdminComplaintDetails(
						data.departmentAdmin().getCollegeEmail(), complaint.getId()));
	}

	private TestData createTestData() {
		String suffix = UUID.randomUUID().toString();
		Department department = departmentRepository.save(Department.builder()
				.name("Facilities " + suffix)
				.code("TEST-" + suffix)
				.enabled(true)
				.build());
		User departmentAdmin = createUser(
				"Facilities Admin", UserRole.DEPARTMENT_ADMIN.name(), null, List.of(department));
		User student = createUser(
				"Campus Student", UserRole.STUDENT.name(), "ROLL-" + suffix, List.of());
		User worker = createUser(
				"Facilities Worker", UserRole.WORKER.name(), null, List.of(department));
		User mainAdmin = createUser(
				"Main Admin", UserRole.ADMIN.name(), null, List.of());
		return new TestData(department, departmentAdmin, student, worker, mainAdmin);
	}

	private User createUser(String name, String role, String rollNo, List<Department> departments) {
		String email = UUID.randomUUID() + "@college.edu";
		return userRepository.save(User.builder()
				.name(name)
				.collegeEmail(email)
				.password("not-used-by-this-service")
				.role(UserRole.valueOf(role))
				.rollNo(rollNo)
				.enabled(true)
				.departments(new HashSet<>(departments))
				.build());
	}

	private record TestData(
			Department department,
			User departmentAdmin,
			User student,
			User worker,
			User mainAdmin) {
	}
}
