package com.nevgiu.hrai.candidate.ingestion.dto;

import java.util.List;

public record CvArchiveImportResult(
        int totalFiles,
        int imported,
        int duplicates,
        int needsReview,
        int skipped,
        int failed,
        List<CvImportResult> results
) {}
