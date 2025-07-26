package com.think41.backend.controller;

import com.think41.backend.DTO.ChatRequest;
import com.think41.backend.DTO.ChatResponse;
import com.think41.backend.services.ChatService; // Ensure this import is correct
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> handleChat(@RequestBody ChatRequest request) {
        if (request.getUserId() == null || request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        try {
            ChatResponse response = chatService.handleChatMessage(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException | SecurityException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ChatResponse(null, null, e.getMessage(), null, null));
        } catch (Exception e) {
            System.err.println("Error processing chat message: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ChatResponse(null, null, "An internal server error occurred: " + e.getMessage(), null, null));
        }
    }

    @GetMapping("/conversations/{sessionId}")
    public ResponseEntity<?> getConversationHistory(@PathVariable Long sessionId) {
        try {
            return ResponseEntity.ok(chatService.getConversationHistory(sessionId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving conversation history.");
        }
    }
}