package com.nevgiu.hrai.chat;

import com.nevgiu.hrai.candidate.Candidate;
import com.nevgiu.hrai.candidate.CandidateRepository;
import com.nevgiu.hrai.chat.dto.CandidateSummaryDto;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ChatServiceTest {

    @Test
    void buildRetrievalText_includesHistoryAndNewMessage() {
        ChatSessionRepository sessionRepo = null;
        ChatMessageRepository msgRepo = null;
        CandidateRepository candRepo = null;
        ChatClient chatClient = null;

        ChatService service = new ChatService(sessionRepo, msgRepo, candRepo, chatClient);

        ChatSession session = new ChatSession();
        ChatMessage m1 = new ChatMessage(session, "USER", "Find Python devs", Instant.now());
        ChatMessage m2 = new ChatMessage(session, "AI", "Here are some options", Instant.now());

        String text = service.buildRetrievalText(List.of(m2, m1), "Who has AWS experience?");

        assertThat(text).contains("USER: Find Python devs");
        assertThat(text).contains("AI: Here are some options");
        assertThat(text).contains("USER: Who has AWS experience?");
    }

    @Test
    void buildCandidatesContext_formatsCandidates() {
        ChatSessionRepository sessionRepo = null;
        ChatMessageRepository msgRepo = null;
        CandidateRepository candRepo = null;
        ChatClient chatClient = null;

        ChatService service = new ChatService(sessionRepo, msgRepo, candRepo, chatClient);

        Candidate c = new Candidate();
        c.setId(1L);
        c.setName("Alice");
        c.setCurrentTitle("Senior Python Developer");
        c.setLocation("Lisbon");
        c.setYearsExperience(7);
        c.setCvText("Python, Django, AWS...");

        String ctx = service.buildCandidatesContext(List.of(c));

        assertThat(ctx).contains("id=1");
        assertThat(ctx).contains("Alice");
        assertThat(ctx).contains("Senior Python Developer");
        assertThat(ctx).contains("Python, Django, AWS");
    }

    @Test
    void mapToSummaries_assignsDecreasingRelevanceScores() {
        ChatSessionRepository sessionRepo = null;
        ChatMessageRepository msgRepo = null;
        CandidateRepository candRepo = null;
        ChatClient chatClient = null;

        ChatService service = new ChatService(sessionRepo, msgRepo, candRepo, chatClient);

        Candidate c1 = new Candidate();
        c1.setId(1L);
        c1.setName("Alice");
        c1.setCurrentTitle("Dev1");
        c1.setLocation("Lisbon");
        c1.setYearsExperience(5);

        Candidate c2 = new Candidate();
        c2.setId(2L);
        c2.setName("Bob");
        c2.setCurrentTitle("Dev2");
        c2.setLocation("Porto");
        c2.setYearsExperience(6);

        List<CandidateSummaryDto> list = service.mapToSummaries(List.of(c1, c2));

        assertThat(list).hasSize(2);
        assertThat(list.get(0).relevanceScore()).isGreaterThan(list.get(1).relevanceScore());
    }
}

