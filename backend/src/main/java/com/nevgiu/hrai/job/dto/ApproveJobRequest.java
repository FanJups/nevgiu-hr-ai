package com.nevgiu.hrai.job.dto;

public record ApproveJobRequest(
        JobGenerationRequest originalRequest,
        GeneratedJobOffer finalJobOffer
) {}
