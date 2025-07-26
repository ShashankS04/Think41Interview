package com.think41.backend.services;


import com.think41.backend.DTO.ChatRequest;
import com.think41.backend.DTO.ChatResponse;
import com.think41.backend.entity.ChatMessage;
import com.think41.backend.entity.ConversationSession;
import com.think41.backend.entity.User;
import com.think41.backend.Repo.ChatMessageRepository;
import com.think41.backend.Repo.ConversationSessionRepository;
import com.think41.backend.Repo.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Optional;

@Service
public class ChatService {

    private final UserRepository userRepository;
    private final ConversationSessionRepository conversationSessionRepository;
    private final ChatMessageRepository chatMessageRepository;

    public ChatService(UserRepository userRepository,
                       ConversationSessionRepository conversationSessionRepository,
                       ChatMessageRepository chatMessageRepository) {
        this.userRepository = userRepository;
        this.conversationSessionRepository = conversationSessionRepository;
        this.chatMessageRepository = chatMessageRepository;
    }

    @Transactional // Ensure all DB operations are atomic
    public ChatResponse handleChatMessage(ChatRequest request) {
        // 1. Validate User
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + request.getUserId()));

        // 2. Determine Conversation Session
        ConversationSession session;
        if (request.getConversationId() != null) {
            // Attempt to load existing session
            session = conversationSessionRepository.findById(request.getConversationId())
                    .orElseThrow(() -> new IllegalArgumentException("Conversation session not found with ID: " + request.getConversationId()));
            // Optional: Add logic to verify if the session belongs to the user
            if (!session.getUser().getId().equals(user.getId())) {
                throw new SecurityException("Unauthorized: Session does not belong to the user.");
            }
            // Optional: If session is "CLOSED", start a new one, or re-open it
            if ("CLOSED".equals(session.getStatus()) || "EXPIRED".equals(session.getStatus())) {
                session.setStatus("ACTIVE"); // Re-activate
                session = conversationSessionRepository.save(session); // Save changes
            }

        } else {
            // Start a new conversation session
            session = new ConversationSession(user);
            session = conversationSessionRepository.save(session);
        }

        // 3. Persist User's Message
        int userSequenceNumber = getNextSequenceNumber(session);
        ChatMessage userMessage = new ChatMessage();
        userMessage.setSession(session);
        userMessage.setSenderType(ChatMessage.SenderType.USER);
        userMessage.setMessageContent(request.getMessage());
        userMessage.setTimestamp(LocalDateTime.now());
        userMessage.setSequenceNumber(userSequenceNumber);
        chatMessageRepository.save(userMessage);

        // 4. Simulate AI Response (THIS IS WHERE YOUR ACTUAL AI INTEGRATION WOULD GO)
        String aiResponseContent = generateAiResponse(request.getMessage());
        // In a real scenario, this would involve calling your actual AI model.
        // For now, it's a simple echo or hardcoded response.

        // 5. Persist AI's Response
        int aiSequenceNumber = getNextSequenceNumber(session);
        ChatMessage aiMessage = new ChatMessage();
        aiMessage.setSession(session);
        aiMessage.setSenderType(ChatMessage.SenderType.AI);
        aiMessage.setMessageContent(aiResponseContent);
        aiMessage.setTimestamp(LocalDateTime.now());
        aiMessage.setSequenceNumber(aiSequenceNumber);
        chatMessageRepository.save(aiMessage);

        // 6. Update session end time (optional, can be done periodically or on explicit end)
        session.setEndTime(LocalDateTime.now());
        conversationSessionRepository.save(session); // Save updated session

        // 7. Return ChatResponse
        return new ChatResponse(
                session.getId(),
                aiMessage.getId(),
                aiMessage.getMessageContent(),
                aiMessage.getTimestamp(),
                aiMessage.getSenderType()
        );
    }

    // Helper method to determine the next sequence number for a message in a session
    private int getNextSequenceNumber(ConversationSession session) {
        // Retrieve all messages for the session to find the max sequence number
        // This is not the most performant for very long conversations.
        // A dedicated sequence counter in ConversationSession could be better for large scale.
        Optional<ChatMessage> lastMessage = chatMessageRepository.findBySessionOrderBySequenceNumberAsc(session)
                .stream()
                .max(Comparator.comparing(ChatMessage::getSequenceNumber));

        return lastMessage.map(msg -> msg.getSequenceNumber() + 1).orElse(1);
    }


    // --- Placeholder for AI Integration ---
    private String generateAiResponse(String userMessage) {
        // **** THIS IS THE MOCK/PLACEHOLDER FOR YOUR AI AGENT CALL ****
        // In a real application, you would:
        // 1. Call your conversational AI model (e.g., Google's Gemini API, OpenAI GPT, your custom model).
        // 2. Pass userMessage and potentially the conversation history.
        // 3. Receive the AI's generated response.

        // Simple echo for now, or product lookups
        if (userMessage.toLowerCase().contains("hello")) {
            return "Hello! How can I assist you with your shopping today?";
        } else if (userMessage.toLowerCase().contains("order status")) {
            return "Please provide your order ID, and I can check its status for you.";
        } else if (userMessage.toLowerCase().contains("product")) {
            return "What kind of product are you looking for?";
        } else if (userMessage.toLowerCase().contains("thank you") || userMessage.toLowerCase().contains("thanks")) {
            return "You're welcome! Is there anything else I can help with?";
        }
        return "I received your message: \"" + userMessage + "\". How else can I help you?";
    }

    // You might also want a method to retrieve conversation history
    @Transactional(readOnly = true)
    public ConversationSession getConversationHistory(Long sessionId) {
        return conversationSessionRepository.findById(sessionId)
                .map(session -> {
                    // Eagerly fetch messages if not already fetched due to LAZY loading
                    // This often requires specific fetch joins or accessing the collection within a transaction
                    session.getMessages().size(); // Forces initialization
                    return session;
                })
                .orElseThrow(() -> new IllegalArgumentException("Conversation session not found with ID: " + sessionId));
    }
}