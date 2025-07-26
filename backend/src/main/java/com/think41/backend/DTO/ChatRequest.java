package com.think41.backend.DTO;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {
    private Long userId; // Required: who is sending the message
    private String message; // Required: the user's message
    private Long conversationId; // Optional: if continuing an existing conversation
}