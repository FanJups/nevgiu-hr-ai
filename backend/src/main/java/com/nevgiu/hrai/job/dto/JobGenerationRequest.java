package com.nevgiu.hrai.job.dto;

public record JobGenerationRequest(
        String briefDescription,
        String department,
        String location,
        String employmentType,   // FULL_TIME, PART_TIME, CONTRACT...
        String salaryRange,
        String tone              // "formal", "friendly", "inclusive"
) {}
