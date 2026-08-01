package com.nevgiu.hrai.evaluation.dto;

import jakarta.validation.constraints.PositiveOrZero;

public record EvaluationWeights(
        @PositiveOrZero double skillsWeight,
        @PositiveOrZero double experienceWeight,
        @PositiveOrZero double educationWeight,
        @PositiveOrZero double achievementWeight,
        @PositiveOrZero double qualityWeight,
        @PositiveOrZero double gapWeight,
        @PositiveOrZero double readabilityWeight,
        @PositiveOrZero double confidenceWeight
) {}
