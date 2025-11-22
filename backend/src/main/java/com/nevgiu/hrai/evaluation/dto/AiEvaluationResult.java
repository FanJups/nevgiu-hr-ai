package com.nevgiu.hrai.evaluation.dto;

public record AiEvaluationResult(
        Scores scores,
        String explanation
) {
    public record Scores(
            int skillsMatchScore,
            int experienceRelevanceScore,
            int educationFitScore,
            int achievementImpactScore,
            int keywordDensityScore,
            int employmentGapScore,
            int readabilityScore,
            int aiConfidenceScore
    ) {}
}
