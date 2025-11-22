package com.nevgiu.hrai.job.dto;

import java.util.List;

public record JobGenerationResponse(
        GeneratedJobOffer jobOffer,
        List<String> missingInfo,
        List<String> suggestions
) {}
