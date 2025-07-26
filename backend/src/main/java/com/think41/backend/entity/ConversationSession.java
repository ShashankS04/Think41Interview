package com.think41.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime; // Use LocalDateTime for timestamps in this context

@Entity
@Table(name = "conversation_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversationSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Database generates ID
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) // Many sessions to one user
    @JoinColumn(name = "user_id", nullable = false) // Link to your existing User entity
    private User user;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime; // Nullable, as session might still be active

    private String title; // Optional: auto-generated or user-defined title for the session
    private String status; // e.g., ACTIVE, CLOSED, EXPIRED

    // A list of messages in this session.
    // MappedBy indicates the owning side of the relationship is in ChatMessage
    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("timestamp ASC, sequenceNumber ASC") // Order messages chronologically
    private java.util.List<ChatMessage> messages;

    // Constructor to start a new session
    public ConversationSession(User user) {
        this.user = user;
        this.startTime = LocalDateTime.now();
        this.status = "ACTIVE"; // Default status
    }
}