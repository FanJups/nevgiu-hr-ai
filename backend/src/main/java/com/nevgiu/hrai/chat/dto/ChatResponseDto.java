package com.nevgiu.hrai.chat.dto;

import java.util.List;

public record ChatResponseDto(
        Long sessionId,
        String answer,
        List<CandidateSummaryDto> candidates
) {}
