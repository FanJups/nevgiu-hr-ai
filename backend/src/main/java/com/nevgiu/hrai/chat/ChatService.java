package com.nevgiu.hrai.chat;

import com.nevgiu.hrai.candidate.Candidate;
import com.nevgiu.hrai.candidate.CandidateRepository;
import com.nevgiu.hrai.chat.dto.CandidateSummaryDto;
import com.nevgiu.hrai.chat.dto.ChatRequestDto;
import com.nevgiu.hrai.chat.dto.ChatResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final CandidateRepository candidateRepository;
    private final ChatClient chatClient;

    public ChatResponseDto handleWebMessage(ChatRequestDto req) {
        ChatSession session = resolveSession("WEB", req.userId(), req.sessionId());

        List<ChatMessage> history = messageRepository.findTop10BySessionOrderByCreatedAtDesc(session);

        ChatMessage userMsg = new ChatMessage(session, "USER", req.message(), Instant.now());
        messageRepository.save(userMsg);
        session.setLastActivityAt(Instant.now());
        sessionRepository.save(session);

        String retrievalText = buildRetrievalText(history, req.message());

        List<Candidate> retrieved = candidateRepository.findAll(PageRequest.of(0, 10)).getContent();

        String systemPrompt = buildSystemPrompt();
        String candidatesContext = buildCandidatesContext(retrieved);

        String llmInput = "CONTEXT:\n" + candidatesContext + "\n\nCHAT HISTORY AND QUESTION:\n" + retrievalText;

        String answer = chatClient
                .prompt()
                .system(systemPrompt)
                .user(llmInput)
                .call()
                .content();

        ChatMessage aiMsg = new ChatMessage(session, "AI", answer, Instant.now());
        messageRepository.save(aiMsg);
        session.setLastActivityAt(Instant.now());
        sessionRepository.save(session);

        List<CandidateSummaryDto> summaries = mapToSummaries(retrieved);

        return new ChatResponseDto(session.getId(), answer, summaries);
    }

    ChatSession resolveSession(String channel, String userIdentifier, Long sessionId) {
        if (sessionId != null) {
            return sessionRepository.findById(sessionId)
                    .orElseThrow(() -> new IllegalArgumentException("Session not found"));
        }
        ChatSession session = new ChatSession();
        session.setChannel(channel);
        session.setUserIdentifier(userIdentifier);
        session.setCreatedAt(Instant.now());
        session.setLastActivityAt(Instant.now());
        return sessionRepository.save(session);
    }

    String buildRetrievalText(List<ChatMessage> historyDesc, String newMessage) {
        if (historyDesc == null) {
            historyDesc = Collections.emptyList();
        }
        List<ChatMessage> reversed = new ArrayList<>(historyDesc);
        Collections.reverse(reversed);

        StringBuilder sb = new StringBuilder();
        for (ChatMessage msg : reversed) {
            sb.append(msg.getSender())
                    .append(": ")
                    .append(msg.getContent())
                    .append("\n");
        }
        sb.append("USER: ").append(newMessage);
        return sb.toString();
    }

    String buildSystemPrompt() {
        return ""
                + "You are an AI assistant helping recruiters explore a candidate database.\n"
                + "You receive a user query, chat history, and a list of candidate profiles.\n"
                + "Answer ONLY based on the provided candidates.\n"
                + "If no candidates seem relevant, say so and ask for clarification or broader criteria.\n"
                + "When listing candidates, mention their name, title, location, and why they match.\n";
    }

    String buildCandidatesContext(List<Candidate> candidates) {
        StringBuilder sb = new StringBuilder();
        int index = 1;
        for (Candidate c : candidates) {
            sb.append("[")
                    .append(index)
                    .append("] id=")
                    .append(c.getId())
                    .append(", name=")
                    .append(c.getName())
                    .append(", title=")
                    .append(c.getCurrentTitle())
                    .append(", location=")
                    .append(c.getLocation())
                    .append(", yearsExperience=")
                    .append(c.getYearsExperience())
                    .append("\nSummary of CV: ")
                    .append(c.getCvText())
                    .append("\n\n");
            index++;
        }
        return sb.toString();
    }

    List<CandidateSummaryDto> mapToSummaries(List<Candidate> candidates) {
        List<CandidateSummaryDto> list = new ArrayList<>();
        int score = 100;
        for (Candidate c : candidates) {
            list.add(new CandidateSummaryDto(
                    c.getId(),
                    c.getName(),
                    c.getCurrentTitle(),
                    c.getLocation(),
                    c.getYearsExperience(),
                    score
            ));
            score = Math.max(0, score - 5);
        }
        return list;
    }
}
