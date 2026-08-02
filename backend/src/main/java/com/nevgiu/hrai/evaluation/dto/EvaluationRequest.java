package com.nevgiu.hrai.evaluation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record EvaluationRequest(
        @NotNull Long candidateId,
        @NotNull Long jobId,
        @Valid EvaluationWeights weights
) {}
