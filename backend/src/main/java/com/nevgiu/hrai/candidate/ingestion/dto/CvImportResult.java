package com.nevgiu.hrai.candidate.ingestion.dto;

import com.nevgiu.hrai.candidate.CvIngestionStatus;

import java.util.List;

public record CvImportResult(
        Long candidateId,
        Long documentId,
        String originalFilename,
        CvIngestionStatus status,
        String contentType,
        int textLength,
        List<String> warnings
) {}
