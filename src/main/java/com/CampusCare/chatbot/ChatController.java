package com.CampusCare.chatbot;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chatbot")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/messages")
    public ResponseEntity<java.util.List<ChatMessageResponse>> history(
            Authentication authentication) {
        return ResponseEntity.ok(chatService.history(authentication.getName()));
    }

    @PostMapping("/messages")
    public ResponseEntity<ChatResponse> reply(
            @Valid @RequestBody ChatRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(chatService.reply(authentication.getName(), request));
    }
}