package com.CampusCare.analytics;

import com.CampusCare.complaint.Complaint;
import com.CampusCare.complaint.ComplaintRepository;
import com.CampusCare.complaint.ComplaintStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final ComplaintRepository complaintRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> summary() {
	var complaints = complaintRepository.findAll();
	Map<ComplaintStatus, Long> byStatus = complaints.stream()
		.collect(Collectors.groupingBy(Complaint::getStatus, Collectors.counting()));
	Map<String, Long> byDepartment = complaints.stream()
		.filter(complaint -> complaint.getDepartment() != null)
		.collect(Collectors.groupingBy(
			complaint -> complaint.getDepartment().getName(), Collectors.counting()));
	long resolved = byStatus.getOrDefault(ComplaintStatus.RESOLVED, 0L);
	long open = complaints.size() - resolved
		- byStatus.getOrDefault(ComplaintStatus.CLOSED, 0L);
	return Map.of(
		"totalComplaints", complaints.size(),
		"resolvedComplaints", resolved,
		"openComplaints", Math.max(open, 0),
		"complaintsByStatus", byStatus,
		"complaintsByDepartment", byDepartment);
    }
}
