package com.nevgiu.hrai.chat.dto;

public record CandidateSummaryDto(
        Long id,
        String name,
        String currentTitle,
        String location,
        Integer yearsExperience,
        int relevanceScore
) {}
