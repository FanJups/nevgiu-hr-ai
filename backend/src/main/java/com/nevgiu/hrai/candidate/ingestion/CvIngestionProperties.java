package com.nevgiu.hrai.candidate.ingestion;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.cv-ingestion")
public record CvIngestionProperties(
        long maxPdfSize,
        long maxArchiveSize,
        int maxArchiveEntries,
        long maxExpandedSize,
        double maxCompressionRatio,
        int minimumTextLength,
        String initialResource,
        boolean initialImportEnabled
) {}
