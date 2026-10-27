package com.CampusCare.chatbot;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class ChatResponse {
    Long id;
    ChatMessage.Sender sender;
    LocalDateTime createdAt;
    String reply;
}
