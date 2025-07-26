package com.think41.backend.services;


import com.think41.backend.DTO.ChatRequest;
import com.think41.backend.DTO.ChatResponse;
import com.think41.backend.entity.ChatMessage;
import com.think41.backend.entity.ConversationSession;
import com.think41.backend.entity.User;
import com.think41.backend.Repo.ChatMessageRepository;
import com.think41.backend.Repo.ConversationSessionRepository;
import com.think41.backend.Repo.UserRepository;
import com.think41.backend.services.GroqApiClient; // Import Groq API Client
import com.think41.backend.entity.Product; // Assuming you need Product
import com.think41.backend.Repo.ProductRepository; // Assuming you need ProductRepository
import com.think41.backend.Repo.OrderRepository; // Assuming you need OrderRepository
import com.think41.backend.entity.Order; // Assuming you need Order

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern; // For parsing LLM's query requests

@Service
public class ChatService {

    private final UserRepository userRepository;
    private final ConversationSessionRepository conversationSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final GroqApiClient groqApiClient; // Inject GroqApiClient
    private final ProductRepository productRepository; // Inject ProductRepository
    private final OrderRepository orderRepository; // Inject OrderRepository

    // Define the system prompt for the LLM
    private static final String SYSTEM_PROMPT = """
        You are an intelligent e-commerce assistant.
        Your primary goal is to help users with their shopping inquiries, order statuses, and product information.

        Capabilities:
        1.  **Answer questions about products:** You can look up products by name, category, or brand.
        2.  **Check order status:** You can check the status of an order if the user provides an order ID.
        3.  **Ask clarifying questions:** If you need more information to fulfill a request (e.g., "Which product are you interested in?"), ask the user.
        4.  **Use Tools (Simulated):** If you need to query the database for product or order information, respond with a specific JSON format.
            -   **To search for products:**
                ```json
                {"tool": "search_products", "query": "product_name_or_category"}
                ```
                Example: `{"tool": "search_products", "query": "laptop"}` or `{"tool": "search_products", "query": "Electronics"}`
            -   **To check order status:**
                ```json
                {"tool": "check_order_status", "order_id": 12345}
                ```
                Example: `{"tool": "check_order_status", "order_id": 12345}`
        5.  **Formulate informative responses:** Once you have the information, provide a helpful and concise answer.
        6.  **Maintain conversation context:** Remember previous turns.

        Examples of interaction:
        User: "I'm looking for a new phone."
        Assistant: "I can help with that! Do you have a specific brand or model in mind?"

        User: "Check order 54321."
        Assistant: `{"tool": "check_order_status", "order_id": 54321}`

        Tool Output: Order 54321 is currently 'SHIPPED'.
        Assistant: "Your order 54321 is currently shipped and on its way."

        Strictly adhere to the tool output format. Do not invent information.
        """;

    // Regex to detect and extract tool calls from LLM response
    private static final Pattern TOOL_CALL_PATTERN = Pattern.compile("\\{\\s*\"tool\"\\s*:\\s*\"([a-zA-Z_]+)\"\\s*,\\s*\"([a-zA-Z_]+)\"\\s*:\\s*\"?([a-zA-Z0-9 ]+)\"?\\s*\\}");

    public ChatService(UserRepository userRepository,
                       ConversationSessionRepository conversationSessionRepository,
                       ChatMessageRepository chatMessageRepository,
                       GroqApiClient groqApiClient,
                       ProductRepository productRepository,
                       OrderRepository orderRepository) {
        this.userRepository = userRepository;
        this.conversationSessionRepository = conversationSessionRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.groqApiClient = groqApiClient;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional
    public ChatResponse handleChatMessage(ChatRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + request.getUserId()));

        ConversationSession session;
        if (request.getConversationId() != null) {
            session = conversationSessionRepository.findById(request.getConversationId())
                    .orElseThrow(() -> new IllegalArgumentException("Conversation session not found with ID: " + request.getConversationId()));
            if (!session.getUser().getId().equals(user.getId())) {
                throw new SecurityException("Unauthorized: Session does not belong to the user.");
            }
            if ("CLOSED".equals(session.getStatus()) || "EXPIRED".equals(session.getStatus())) {
                session.setStatus("ACTIVE");
                session = conversationSessionRepository.save(session);
            }
        } else {
            session = new ConversationSession(user);
            session = conversationSessionRepository.save(session);
        }

        // 1. Persist User's Message
        int userSequenceNumber = getNextSequenceNumber(session);
        ChatMessage userMessage = new ChatMessage();
        userMessage.setSession(session);
        userMessage.setSenderType(ChatMessage.SenderType.USER);
        userMessage.setMessageContent(request.getMessage());
        userMessage.setTimestamp(LocalDateTime.now());
        userMessage.setSequenceNumber(userSequenceNumber);
        chatMessageRepository.save(userMessage);

        // 2. Get Conversation History for LLM
        // Important: fetch messages within the same transaction or ensure they are loaded
        List<ChatMessage> historyMessages = chatMessageRepository.findBySessionOrderBySequenceNumberAsc(session);

        List<Map<String, String>> llmMessages = new ArrayList<>();
        llmMessages.add(Map.of("role", "system", "content", SYSTEM_PROMPT)); // System prompt first

        // Add previous messages
        for (ChatMessage msg : historyMessages) {
            llmMessages.add(Map.of(
                    "role", msg.getSenderType().toString().toLowerCase(),
                    "content", msg.getMessageContent()
            ));
        }
        // Add current user message
        llmMessages.add(Map.of("role", "user", "content", request.getMessage()));


        // 3. Call Groq API (blocking for simplicity; consider reactive if needed)
        String llmRawResponse = groqApiClient.getChatCompletion(llmMessages).block(); // .block() for synchronous call

        String finalAiResponseContent;
        if (llmRawResponse != null && isToolCall(llmRawResponse)) {
            // LLM wants to use a tool
            System.out.println("LLM requested tool call: " + llmRawResponse);
            String toolOutput = executeToolCall(llmRawResponse);

            // Send tool output back to LLM for final response generation
            llmMessages.add(Map.of("role", "tool", "content", toolOutput)); // "tool" role might vary, check Groq docs if issues
            llmMessages.add(Map.of("role", "user", "content", "Based on the following tool output, please provide a comprehensive answer: " + toolOutput));
            finalAiResponseContent = groqApiClient.getChatCompletion(llmMessages).block();
            System.out.println("LLM final response after tool: " + finalAiResponseContent);

        } else {
            // LLM provided a direct response or clarifying question
            finalAiResponseContent = llmRawResponse;
            System.out.println("LLM direct response: " + finalAiResponseContent);
        }

        // 4. Persist AI's Response
        int aiSequenceNumber = getNextSequenceNumber(session);
        ChatMessage aiMessage = new ChatMessage();
        aiMessage.setSession(session);
        aiMessage.setSenderType(ChatMessage.SenderType.AI);
        aiMessage.setMessageContent(finalAiResponseContent != null ? finalAiResponseContent : "I'm sorry, I couldn't generate a response.");
        aiMessage.setTimestamp(LocalDateTime.now());
        aiMessage.setSequenceNumber(aiSequenceNumber);
        chatMessageRepository.save(aiMessage);

        session.setEndTime(LocalDateTime.now()); // Update session end time
        conversationSessionRepository.save(session);

        return new ChatResponse(
                session.getId(),
                aiMessage.getId(),
                aiMessage.getMessageContent(),
                aiMessage.getTimestamp(),
                aiMessage.getSenderType()
        );
    }

    private int getNextSequenceNumber(ConversationSession session) {
        return chatMessageRepository.findBySessionOrderBySequenceNumberAsc(session)
                .stream()
                .map(ChatMessage::getSequenceNumber)
                .max(Integer::compare)
                .orElse(0) + 1; // Start from 1 if no messages, else max + 1
    }

    // --- Tool Use Simulation Logic ---
    private boolean isToolCall(String llmResponse) {
        return llmResponse != null && llmResponse.trim().startsWith("{\"tool\":");
    }

    private String executeToolCall(String llmResponse) {
        Matcher matcher = TOOL_CALL_PATTERN.matcher(llmResponse.trim());
        if (matcher.find()) {
            String toolName = matcher.group(1);
            String paramName = matcher.group(2);
            String paramValue = matcher.group(3);

            try {
                switch (toolName) {
                    case "search_products":
                        return searchProducts(paramValue);
                    case "check_order_status":
                        return checkOrderStatus(Long.parseLong(paramValue));
                    default:
                        return "Unknown tool: " + toolName;
                }
            } catch (NumberFormatException e) {
                return "Invalid number format for tool parameter: " + paramValue;
            } catch (Exception e) {
                return "Error executing tool '" + toolName + "': " + e.getMessage();
            }
        }
        return "Could not parse tool call from LLM response.";
    }

    // --- Database Query Methods ---
    @Transactional(readOnly = true)
    private String searchProducts(String query) {
        // Search products by name or category
        List<Product> products = productRepository.findByNameContainingIgnoreCaseOrCategoryContainingIgnoreCase(query, query);
        if (products.isEmpty()) {
            return "No products found matching '" + query + "'.";
        }
        return products.stream()
                .limit(5) // Limit results for brevity in response
                .map(p -> String.format("%s (Brand: %s, Price: $%.2f, Category: %s)",
                        p.getName(), p.getBrand(), p.getRetailPrice(), p.getCategory()))
                .collect(Collectors.joining("\n- ", "Found the following products:\n- ", ""));
    }

    @Transactional(readOnly = true)
    private String checkOrderStatus(Long orderId) {
        Optional<Order> orderOptional = orderRepository.findById(orderId);
        if (orderOptional.isEmpty()) {
            return "Order with ID " + orderId + " not found.";
        }
        Order order = orderOptional.get();
        return String.format("Order %d is currently '%s' and was created on %s. It contains %d items.",
                order.getId(), order.getStatus(), order.getCreatedAt().toLocalDate(), order.getNumOfItem());
    }

    // You might also want a method to retrieve conversation history
    @Transactional(readOnly = true)
    public ConversationSession getConversationHistory(Long sessionId) {
        return conversationSessionRepository.findById(sessionId)
                .map(session -> {
                    // Eagerly fetch messages within the transaction to avoid LazyInitializationException
                    session.getMessages().size();
                    return session;
                })
                .orElseThrow(() -> new IllegalArgumentException("Conversation session not found with ID: " + sessionId));
    }
}