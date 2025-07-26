package com.think41.backend.DTO;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import com.think41.backend.entity.ChatMessage.SenderType; // Import the enum

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    private Long conversationId;
    private Long messageId; // ID of the AI's response message
    private String response; // The AI's generated response
    private LocalDateTime timestamp;
    private SenderType sender; // Should be AI
    // Potentially add more fields like sentiment, intent, etc.
}