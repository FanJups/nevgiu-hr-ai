package com.nevgiu.hrai.evaluation.dto;

public record EvaluationResponse(
        CandidateEvaluationDto evaluation,
        String explanation
) {}
