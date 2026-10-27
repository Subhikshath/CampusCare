package com.CampusCare.analytics;

import com.CampusCare.complaint.ComplaintRepository;
import com.CampusCare.complaint.ComplaintStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.EnumMap;
import java.util.LinkedHashMap;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final ComplaintRepository complaintRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> summary() {
	Map<ComplaintStatus, Long> byStatus = new EnumMap<>(ComplaintStatus.class);
	long total = 0;
	for (ComplaintStatus status : ComplaintStatus.values()) {
		long count = complaintRepository.countByStatus(status);
		byStatus.put(status, count);
		total += count;
	}
	Map<String, Long> byDepartment = new LinkedHashMap<>();
	for (Object[] row : complaintRepository.countByDepartment()) {
		byDepartment.put((String) row[0], ((Number) row[1]).longValue());
	}
	long resolved = byStatus.getOrDefault(ComplaintStatus.RESOLVED, 0L);
	long closed = byStatus.getOrDefault(ComplaintStatus.CLOSED, 0L);
	long open = total - resolved - closed;
	return Map.of(
		"totalComplaints", total,
		"resolvedComplaints", resolved,
		"closedComplaints", closed,
		"pendingComplaints", Math.max(open, 0),
		"rejectedComplaints", byStatus.getOrDefault(ComplaintStatus.REJECTED, 0L),
		"openComplaints", Math.max(open, 0),
		"complaintsByStatus", byStatus,
		"complaintsByDepartment", byDepartment);
    }
}
