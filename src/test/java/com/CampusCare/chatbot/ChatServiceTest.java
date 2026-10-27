package com.CampusCare.chatbot;

import com.CampusCare.ai.GeminiClient;
import com.CampusCare.user.User;
import com.CampusCare.user.UserRepository;
import com.CampusCare.user.UserRole;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

class ChatServiceTest {

	@Test
	void storesQuestionAndGroundedAnswerForTheAuthenticatedUser() {
		User user = user();
		ChatMessageStore store = mock(ChatMessageStore.class);
		when(store.findRecentMessages(user.getId())).thenReturn(List.of());
		when(store.save(eq(user), eq(ChatMessage.Sender.USER), anyString()))
				.thenReturn(message(10L, user, ChatMessage.Sender.USER, "How do I submit a report?"));
		when(store.save(eq(user), eq(ChatMessage.Sender.ASSISTANT), anyString()))
				.thenReturn(message(11L, user, ChatMessage.Sender.ASSISTANT,
						"Choose New complaint."));
		UserRepository users = mock(UserRepository.class);
		when(users.findByCollegeEmail(user.getCollegeEmail())).thenReturn(Optional.of(user));
		GeminiClient gemini = mock(GeminiClient.class);
		when(gemini.chat(contains("Submitting a complaint:"))).thenReturn("Choose New complaint.");

		ChatResponse response = new ChatService(
				gemini, users, store, new PortalKnowledgeBase())
				.reply(user.getCollegeEmail(), request("How do I submit a report?"));

		assertEquals("Choose New complaint.", response.getReply());
		assertEquals(11L, response.getId());
		verify(store).save(user, ChatMessage.Sender.USER, "How do I submit a report?");
		verify(store).save(user, ChatMessage.Sender.ASSISTANT, "Choose New complaint.");
	}

	@Test
	void declinesQuestionsOutsideThePortalWithoutCallingGemini() {
		User user = user();
		ChatMessageStore store = mock(ChatMessageStore.class);
		when(store.findRecentMessages(user.getId())).thenReturn(List.of());
		when(store.save(eq(user), eq(ChatMessage.Sender.USER), anyString()))
				.thenReturn(message(10L, user, ChatMessage.Sender.USER, "What is the weather?"));
		when(store.save(eq(user), eq(ChatMessage.Sender.ASSISTANT), anyString()))
				.thenAnswer(invocation -> message(11L, user, ChatMessage.Sender.ASSISTANT,
						invocation.getArgument(2)));
		UserRepository users = mock(UserRepository.class);
		when(users.findByCollegeEmail(user.getCollegeEmail())).thenReturn(Optional.of(user));
		GeminiClient gemini = mock(GeminiClient.class);

		ChatResponse response = new ChatService(
				gemini, users, store, new PortalKnowledgeBase())
				.reply(user.getCollegeEmail(), request("What is the weather?"));

		assertTrue(response.getReply().contains(
				"only answer questions about using the CampusCare portal"));
		verifyNoInteractions(gemini);
		verify(store).save(eq(user), eq(ChatMessage.Sender.USER), anyString());
		verify(store).save(eq(user), eq(ChatMessage.Sender.ASSISTANT), anyString());
	}

	private ChatRequest request(String text) {
		ChatRequest request = new ChatRequest();
		request.setMessage(text);
		return request;
	}

	private User user() {
		return User.builder()
				.id(1L)
				.name("CampusCare Student")
				.collegeEmail("student@college.edu")
				.password("not-used")
				.role(UserRole.STUDENT)
				.build();
	}

	private ChatMessage message(Long id, User user, ChatMessage.Sender sender, String text) {
		return ChatMessage.builder()
				.id(id)
				.user(user)
				.sender(sender)
				.message(text)
				.createdAt(LocalDateTime.now())
				.build();
	}
}
