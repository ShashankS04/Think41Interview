package com.think41.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) // Many messages to one session
    @JoinColumn(name = "session_id", nullable = false)
    private ConversationSession session;

    @Column(name = "sequence_number", nullable = false)
    private Integer sequenceNumber; // Chronological order within session

    @Enumerated(EnumType.STRING) // Store enum as String in DB
    @Column(name = "sender_type", nullable = false)
    private SenderType senderType; // USER or AI

    @Column(name = "message_content", columnDefinition = "TEXT", nullable = false) // Use TEXT for potentially long messages
    private String messageContent;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "metadata", columnDefinition = "JSONB") // For PostgreSQL JSONB type
    private String metadata; // Store as String (JSON) for simplicity, or use a JSON mapping library

    // Enum for SenderType
    public enum SenderType {
        USER, AI
    }
}