package com.nevgiu.hrai.candidate;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class CvEmbeddingIndexer {

    private final CandidateRepository candidateRepository;
    private final JdbcTemplate jdbcTemplate;
    private final EmbeddingModel embeddingModel;

    @Transactional
    public void reindexAllCandidates() {
        candidateRepository.findAll().forEach(this::indexCandidate);
    }

    @Transactional
    public void indexCandidate(Candidate candidate) {
        if (candidate.getCvText() == null || candidate.getCvText().isBlank()) {
            return;
        }

        // Spring AI 1.1.0 API: embed(List<String>) -> List<float[]>
        List<float[]> vectors = embeddingModel.embed(List.of(candidate.getCvText()));
        if (vectors.isEmpty()) {
            return;
        }

        float[] vector = vectors.get(0);
        String embeddingLiteral = toPgVectorLiteral(vector);

        jdbcTemplate.update(
                "INSERT INTO cv_embeddings(candidate_id, embedding) " +
                        "VALUES (?, CAST(? AS vector)) " +
                        "ON CONFLICT (candidate_id) DO UPDATE SET embedding = EXCLUDED.embedding",
                candidate.getId(),
                embeddingLiteral
        );
    }

    String toPgVectorLiteral(float[] values) {
        List<String> parts = new ArrayList<>(values.length);
        for (float v : values) {
            parts.add(Float.toString(v));
        }
        return "[" + String.join(",", parts) + "]";
    }

    // Optional helper if you ever want List<Double> -> String
    String toPgVectorLiteral(List<Double> values) {
        return "[" + values.stream()
                .map(d -> Double.toString(d))
                .collect(Collectors.joining(",")) + "]";
    }
}
