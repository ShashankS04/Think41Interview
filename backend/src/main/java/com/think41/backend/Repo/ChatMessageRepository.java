package com.think41.backend.Repo;

import com.think41.backend.entity.ChatMessage;
import com.think41.backend.entity.ConversationSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    // Find all messages in a session, ordered chronologically
    List<ChatMessage> findBySessionOrderBySequenceNumberAsc(ConversationSession session);
}