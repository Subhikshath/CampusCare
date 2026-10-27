package com.CampusCare.chatbot;

import com.CampusCare.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatMessageStore {

	private final ChatMessageRepository chatMessageRepository;

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public ChatMessage save(User user, ChatMessage.Sender sender, String message) {
		return chatMessageRepository.save(ChatMessage.builder()
				.user(user)
				.sender(sender)
				.message(message)
				.build());
	}

	@Transactional(readOnly = true)
	public List<ChatMessage> findConversation(Long userId) {
		return chatMessageRepository.findByUserIdOrderByCreatedAtAscIdAsc(userId);
	}

	@Transactional(readOnly = true)
	public List<ChatMessage> findRecentMessages(Long userId) {
		return chatMessageRepository.findTop10ByUserIdOrderByCreatedAtDescIdDesc(userId)
				.stream()
				.sorted((first, second) -> {
					int byCreatedAt = first.getCreatedAt().compareTo(second.getCreatedAt());
					return byCreatedAt != 0 ? byCreatedAt : first.getId().compareTo(second.getId());
				})
				.toList();
	}
}
