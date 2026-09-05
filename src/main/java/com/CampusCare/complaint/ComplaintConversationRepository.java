package com.CampusCare.complaint;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComplaintConversationRepository
        extends JpaRepository<ComplaintConversation, Long> {
    List<ComplaintConversation> findByComplaintIdOrderByCreatedAtAsc(Long complaintId);
}