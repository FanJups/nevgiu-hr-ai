package com.nevgiu.hrai.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nevgiu.hrai.job.dto.ApproveJobRequest;
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


    /**
     * Calls the LLM to generate a job offer draft.
     * NOTE: This NO LONGER saves the job. It only returns the draft.
     */
    public JobGenerationResponse generateJobOffer(JobGenerationRequest request) {
        String systemPrompt = buildSystemPrompt();
        String userPrompt = buildUserPrompt(request);

        String modelResponse = chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .content();

        return parseModelResponse(modelResponse);
    }

    /**
     * Approves a (possibly edited) job offer and persists it as Job.
     */
    public Job approveJob(ApproveJobRequest request) {
        GeneratedJobOffer offer = request.finalJobOffer();
        JobGenerationRequest original = request.originalRequest();

        return saveJob(offer, original);
    }

    String buildUserPrompt(JobGenerationRequest request) {
        return String.format(
                "Brief description: %s%nDepartment: %s%nLocation: %s%nEmployment type: %s%nSalary range: %s%nTone: %s%n",
                request.briefDescription(),
                orNA(request.department()),
                orNA(request.location()),
                orNA(request.employmentType()),
                orNA(request.salaryRange()),
                orNA(request.tone())
        );
    }

    String buildSystemPrompt() {
        return "You are an AI assistant that generates inclusive, well-structured job offers.\n"
                + "Rules:\n"
                + "- Use clear, professional, bias-free language.\n"
                + "- Avoid age-related or gendered terms (e.g., \"young\", \"rockstar\", \"ninja\").\n"
                + "- Adapt tone according to the provided tone parameter (formal, friendly, inclusive).\n"
                + "- Do NOT invent specific salary numbers or benefits if not provided. If missing, mark them as \"missing\".\n"
                + "\n"
                + "You must return a SINGLE JSON object with the following structure:\n"
                + "{\n"
                + "  \"jobOffer\": {\n"
                + "    \"inferredTitle\": \"...\",\n"
                + "    \"level\": \"...\",\n"
                + "    \"summary\": \"...\",\n"
                + "    \"responsibilities\": [\"...\", \"...\"],\n"
                + "    \"requiredQualifications\": [\"...\", \"...\"],\n"
                + "    \"preferredQualifications\": [\"...\", \"...\"],\n"
                + "    \"softSkills\": [\"...\", \"...\"],\n"
                + "    \"benefits\": [\"...\", \"...\"],\n"
                + "    \"employmentType\": \"...\",\n"
                + "    \"location\": \"...\",\n"
                + "    \"salaryRange\": \"...\",\n"
                + "    \"tone\": \"...\"\n"
                + "  },\n"
                + "  \"missingInfo\": [\"salaryRange\", \"location\"],\n"
                + "  \"suggestions\": [\"...\" ]\n"
                + "}\n"
                + "- \"missingInfo\" must list fields that are missing or ambiguous (e.g., salaryRange, location).\n"
                + "- \"suggestions\" should include short tips to improve the job offer, especially for inclusivity & clarity.";
    }

    JobGenerationResponse parseModelResponse(String json) {
        try {
            return objectMapper.readValue(json, JobGenerationResponse.class);
        } catch (IOException e) {
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

    private Job saveJob(GeneratedJobOffer offer, JobGenerationRequest request) {
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

        return jobRepository.save(job);
    }

    private String orNA(String value) {
        return (value == null || value.isBlank()) ? "N/A" : value;
    }
}
