package com.nevgiu.hrai.job.dto;

import java.util.List;

public record GeneratedJobOffer(
        String inferredTitle,
        String level,
        String summary,
        List<String> responsibilities,
        List<String> requiredQualifications,
        List<String> preferredQualifications,
        List<String> softSkills,
        List<String> benefits,
        String employmentType,
        String location,
        String salaryRange,
        String tone
) {}
