package com.think41.backend.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class GroqApiClient {

    private final WebClient webClient;
    private final String modelName;
    private final ObjectMapper objectMapper; // For JSON manipulation

    public GroqApiClient(@Value("${groq.api.url}") String groqApiUrl,
                         @Value("${groq.api.key}") String groqApiKey,
                         @Value("${groq.model.name}") String modelName,
                         WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl(groqApiUrl)
                .defaultHeader("Authorization", "Bearer " + groqApiKey)
                .build();
        this.modelName = modelName;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Calls the Groq API to get a chat completion.
     *
     * @param messages List of messages in the conversation (role: user/assistant, content: message)
     * @return The content of the AI's response message.
     */
    public Mono<String> getChatCompletion(List<Map<String, String>> messages) {
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", modelName);

        ArrayNode messagesNode = objectMapper.createArrayNode();
        messages.forEach(msg -> {
            ObjectNode messageNode = objectMapper.createObjectNode();
            messageNode.put("role", msg.get("role"));
            messageNode.put("content", msg.get("content"));
            messagesNode.add(messageNode);
        });
        requestBody.set("messages", messagesNode);
        requestBody.put("temperature", 0.7); // Adjust as needed
        requestBody.put("max_tokens", 500); // Adjust as needed

        return webClient.post()
                .bodyValue(requestBody.toString())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(jsonNode -> jsonNode.at("/choices/0/message/content").asText())
                .doOnError(e -> System.err.println("Error calling Groq API: " + e.getMessage()));
    }
}