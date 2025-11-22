package com.nevgiu.hrai.evaluation.dto;

public record EvaluationRequest(
        Long candidateId,
        Long jobId,
        EvaluationWeights weights // nullable → use defaults
) {}
