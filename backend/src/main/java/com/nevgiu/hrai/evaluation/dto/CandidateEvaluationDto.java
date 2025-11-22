package com.nevgiu.hrai.evaluation.dto;

import java.time.Instant;

public record CandidateEvaluationDto(
        Long id,
        Long candidateId,
        Long jobId,
        int skillsMatchScore,
        int experienceRelevanceScore,
        int educationFitScore,
        int achievementImpactScore,
        int keywordDensityScore,
        int employmentGapScore,
        int readabilityScore,
        int aiConfidenceScore,
        int overallFitScore,
        String aiExplanation,
        Instant createdAt
) {}
