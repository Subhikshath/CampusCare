package com.CampusCare.chatbot;

import com.CampusCare.user.User;
import com.CampusCare.user.UserRepository;
import com.CampusCare.user.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
class ChatMessageRepositoryTest {

	@Autowired
	private ChatMessageRepository chatMessageRepository;

	@Autowired
	private UserRepository userRepository;

	@Test
	void persistsMessagesAndKeepsEachUsersConversationPrivate() {
		User firstUser = userRepository.save(user("first@college.edu"));
		User secondUser = userRepository.save(user("second@college.edu"));

		chatMessageRepository.save(message(firstUser, ChatMessage.Sender.USER, "How do I report?"));
		chatMessageRepository.save(message(firstUser, ChatMessage.Sender.ASSISTANT, "Choose New complaint."));
		chatMessageRepository.save(message(secondUser, ChatMessage.Sender.USER, "How do I sign in?"));
		chatMessageRepository.flush();

		assertEquals(2, chatMessageRepository
				.findByUserIdOrderByCreatedAtAscIdAsc(firstUser.getId()).size());
		assertEquals("How do I sign in?", chatMessageRepository
				.findByUserIdOrderByCreatedAtAscIdAsc(secondUser.getId()).get(0).getMessage());
	}

	private User user(String email) {
		return User.builder()
				.name("CampusCare Student")
				.collegeEmail(email)
				.password("not-used")
				.role(UserRole.STUDENT)
				.build();
	}

	private ChatMessage message(User user, ChatMessage.Sender sender, String text) {
		return ChatMessage.builder()
				.user(user)
				.sender(sender)
				.message(text)
				.build();
	}
}
