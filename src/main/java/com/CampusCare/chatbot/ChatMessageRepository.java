package com.CampusCare.chatbot;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

	List<ChatMessage> findByUserIdOrderByCreatedAtAscIdAsc(Long userId);

	List<ChatMessage> findTop10ByUserIdOrderByCreatedAtDescIdDesc(Long userId);
}
