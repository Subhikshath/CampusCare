package com.CampusCare.chatbot;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class ChatMessageResponse {
	Long id;
	ChatMessage.Sender sender;
	String message;
	LocalDateTime createdAt;

	public static ChatMessageResponse from(ChatMessage chatMessage) {
		return ChatMessageResponse.builder()
				.id(chatMessage.getId())
				.sender(chatMessage.getSender())
				.message(chatMessage.getMessage())
				.createdAt(chatMessage.getCreatedAt())
				.build();
	}
}
