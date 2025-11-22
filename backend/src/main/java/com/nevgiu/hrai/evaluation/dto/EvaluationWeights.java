package com.nevgiu.hrai.evaluation.dto;

public record EvaluationWeights(
        double skillsWeight,
        double experienceWeight,
        double educationWeight,
        double achievementWeight,
        double qualityWeight,
        double gapWeight,
        double readabilityWeight,
        double confidenceWeight
) {}
