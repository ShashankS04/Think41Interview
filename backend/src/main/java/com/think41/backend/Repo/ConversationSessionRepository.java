package com.think41.backend.Repo;

import com.think41.backend.entity.ConversationSession;
import com.think41.backend.entity.User; // Import your User entity
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversationSessionRepository extends JpaRepository<ConversationSession, Long> {
    // Find sessions for a specific user, ordered by start time
    List<ConversationSession> findByUserOrderByStartTimeDesc(User user);

    // Find active sessions for a user
    List<ConversationSession> findByUserAndStatus(User user, String status);
}