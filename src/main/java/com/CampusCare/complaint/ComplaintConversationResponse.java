package com.CampusCare.complaint;

import java.time.LocalDateTime;

public record ComplaintConversationResponse(
		Long id,
		ComplaintConversation.SenderType senderType,
		String message,
		LocalDateTime createdAt) {

	public static ComplaintConversationResponse from(ComplaintConversation conversation) {
		return new ComplaintConversationResponse(
				conversation.getId(),
				conversation.getSenderType(),
				conversation.getMessage(),
				conversation.getCreatedAt());
	}
}
