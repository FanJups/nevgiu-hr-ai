package com.nevgiu.hrai.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nevgiu.hrai.job.dto.GeneratedJobOffer;
import com.nevgiu.hrai.job.dto.JobGenerationRequest;
import com.nevgiu.hrai.job.dto.JobGenerationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JobGenerationService {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final JobRepository jobRepository;

    public JobGenerationResponse generateJobOffer(JobGenerationRequest request) {
        String systemPrompt = buildSystemPrompt();
        String userPrompt = buildUserPrompt(request);

        String modelResponse = chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .content();

        JobGenerationResponse response = parseModelResponse(modelResponse);

        // Optional: persist generated job
        saveJob(response.jobOffer(), request);

        return response;
    }

    String buildUserPrompt(JobGenerationRequest request) {
        return """
            Brief description: %s
            Department: %s
            Location: %s
            Employment type: %s
            Salary range: %s
            Tone: %s
            """.formatted(
                request.briefDescription(),
                orNA(request.department()),
                orNA(request.location()),
                orNA(request.employmentType()),
                orNA(request.salaryRange()),
                orNA(request.tone())
        );
    }

    String buildSystemPrompt() {
        return """
            You are an AI assistant that generates inclusive, well-structured job offers.
            Rules:
            - Use clear, professional, bias-free language.
            - Avoid age-related or gendered terms (e.g., "young", "rockstar", "ninja").
            - Adapt tone according to the provided tone parameter (formal, friendly, inclusive).
            - Do NOT invent specific salary numbers or benefits if not provided. If missing, mark them as "missing".
                        
            You must return a SINGLE JSON object with the following structure:
            {
              "jobOffer": {
                "inferredTitle": "...",
                "level": "...",
                "summary": "...",
                "responsibilities": ["...", "..."],
                "requiredQualifications": ["...", "..."],
                "preferredQualifications": ["...", "..."],
                "softSkills": ["...", "..."],
                "benefits": ["...", "..."],
                "employmentType": "...",
                "location": "...",
                "salaryRange": "...",
                "tone": "..."
              },
              "missingInfo": ["salaryRange", "location"],
              "suggestions": ["..."]
            }
                        
            - "missingInfo" must list fields that are missing or ambiguous (e.g., salaryRange, location).
            - "suggestions" should include short tips to improve the job offer, especially for inclusivity & clarity.
            """;
    }

    JobGenerationResponse parseModelResponse(String json) {
        try {
            return objectMapper.readValue(json, JobGenerationResponse.class);
        } catch (IOException e) {
            // fallback: very defensive default
            GeneratedJobOffer emptyOffer = new GeneratedJobOffer(
                    "Unknown Title",
                    "",
                    "",
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    "",
                    "",
                    "",
                    "formal"
            );
            return new JobGenerationResponse(emptyOffer, List.of("parseError"), List.of(e.getMessage()));
        }
    }

    private void saveJob(GeneratedJobOffer offer, JobGenerationRequest request) {
        Job job = new Job();
        job.setTitle(offer.inferredTitle());
        job.setLevel(offer.level());
        job.setSummary(offer.summary());
        job.setLocation(offer.location());
        job.setEmploymentType(offer.employmentType());
        job.setSalaryRange(offer.salaryRange());
        job.setDepartment(request.department());
        job.setTone(offer.tone());

        job.setResponsibilities(String.join("\n", offer.responsibilities()));
        job.setRequiredQualifications(String.join("\n", offer.requiredQualifications()));
        job.setPreferredQualifications(String.join("\n", offer.preferredQualifications()));
        job.setSoftSkills(String.join("\n", offer.softSkills()));
        job.setBenefits(String.join("\n", offer.benefits()));

        jobRepository.save(job);
    }

    private String orNA(String value) {
        return (value == null || value.isBlank()) ? "N/A" : value;
    }
}
