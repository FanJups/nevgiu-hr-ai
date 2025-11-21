package com.example.financeai.chat;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    public ChatService(ChatClient chatClient, VectorStore vectorStore) {
        this.chatClient = chatClient;
        this.vectorStore = vectorStore;
    }

    public String ask(String message) {
        if (message == null) {
            message = "";
        }

        List<Document> docs = new ArrayList<>();
        try {
            docs = vectorStore.similaritySearch(message);
        } catch (Exception e) {
            // ignore vector errors for hackathon
        }

        String context = docs.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n"));

        String systemPrompt =
                "You are a helpful assistant that ONLY answers questions " +
                "about finance and banking (loans, interest, investments, " +
                "savings, cards, risk, regulations, etc.). " +
                "If the user asks about anything else, politely say you can " +
                "only help with finance and banking topics.";

        String userPrompt =
                "Here is additional context from user documents (may be empty):\n" +
                context +
                "\n\nUser question:\n" +
                message;

        return chatClient
                .prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .content();
    }

}
