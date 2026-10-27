package com.CampusCare.chatbot;

import com.CampusCare.ai.GeminiClient;
import com.CampusCare.common.ResourceNotFoundException;
import com.CampusCare.user.User;
import com.CampusCare.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final GeminiClient geminiClient;
    private final UserRepository userRepository;
    private final ChatMessageStore chatMessageStore;
    private final PortalKnowledgeBase portalKnowledgeBase;

    public ChatResponse reply(String email, ChatRequest request) {
        User user = findUser(email);
        String question = request.getMessage().trim();
        List<ChatMessage> previousMessages = chatMessageStore.findRecentMessages(user.getId());

        chatMessageStore.save(user, ChatMessage.Sender.USER, question);

        List<String> guides = portalKnowledgeBase.retrieve(question);
        String reply = guides.isEmpty()
                ? "I can only answer questions about using the CampusCare portal. Try asking about submitting or tracking a complaint, campus locations, follow-up questions, notifications, or your account."
                : geminiClient.chat(portalKnowledgeBase.createPrompt(
                        question, guides, previousMessages));

        ChatMessage assistantMessage = chatMessageStore.save(
                user, ChatMessage.Sender.ASSISTANT, reply);
        return ChatResponse.builder()
                .id(assistantMessage.getId())
                .sender(assistantMessage.getSender())
                .createdAt(assistantMessage.getCreatedAt())
                .reply(assistantMessage.getMessage())
                .build();
    }

    public List<ChatMessageResponse> history(String email) {
        User user = findUser(email);
        return chatMessageStore.findConversation(user.getId()).stream()
                .map(ChatMessageResponse::from)
                .toList();
    }

    private User findUser(String email) {
        return userRepository.findByCollegeEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}