package com.nevgiu.hrai.job;

import com.nevgiu.hrai.job.dto.GeneratedJobOffer;
import com.nevgiu.hrai.job.dto.JobGenerationRequest;
import com.nevgiu.hrai.job.dto.JobGenerationResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JobGenerationServiceTest {

    @Test
    void buildUserPrompt_includesAllFields_orNA() {
        JobRepository dummyRepo = null; // not used in these tests
        var service = new JobGenerationService(null, new ObjectMapper(), dummyRepo);

        JobGenerationRequest req = new JobGenerationRequest(
                "Looking for a senior backend developer with Python and AWS experience",
                null,
                "Lisbon",
                "FULL_TIME",
                null,
                "inclusive"
        );

        String prompt = service.buildUserPrompt(req);

        assertThat(prompt).contains("Looking for a senior backend developer");
        assertThat(prompt).contains("Department: N/A");
        assertThat(prompt).contains("Location: Lisbon");
        assertThat(prompt).contains("Employment type: FULL_TIME");
        assertThat(prompt).contains("Salary range: N/A");
        assertThat(prompt).contains("Tone: inclusive");
    }

    @Test
    void parseModelResponse_parsesValidJson() throws Exception {
        JobRepository dummyRepo = null;
        var service = new JobGenerationService(null, new ObjectMapper(), dummyRepo);

        String json = """
            {
              "jobOffer": {
                "inferredTitle": "Senior Backend Developer",
                "level": "Senior",
                "summary": "We are looking for a Senior Backend Developer...",
                "responsibilities": ["Build backend services", "Collaborate with team"],
                "requiredQualifications": ["5+ years experience", "Strong Python skills"],
                "preferredQualifications": ["Experience with AWS", "Docker"],
                "softSkills": ["Teamwork", "Communication"],
                "benefits": ["Health insurance", "Remote work"],
                "employmentType": "FULL_TIME",
                "location": "Lisbon",
                "salaryRange": "Competitive",
                "tone": "inclusive"
              },
              "missingInfo": ["department"],
              "suggestions": ["Consider adding salary range details"]
            }
            """;

        JobGenerationResponse res = service.parseModelResponse(json);

        GeneratedJobOffer offer = res.jobOffer();
        assertThat(offer.inferredTitle()).isEqualTo("Senior Backend Developer");
        assertThat(offer.responsibilities()).hasSize(2);
        assertThat(res.missingInfo()).contains("department");
        assertThat(res.suggestions()).isNotEmpty();
    }

    @Test
    void parseModelResponse_handlesInvalidJsonGracefully() {
        JobRepository dummyRepo = null;
        var service = new JobGenerationService(null, new ObjectMapper(), dummyRepo);

        JobGenerationResponse res = service.parseModelResponse("NOT JSON");

        assertThat(res.jobOffer().inferredTitle()).isEqualTo("Unknown Title");
        assertThat(res.missingInfo()).contains("parseError");
        assertThat(res.suggestions()).isNotEmpty();
    }
}
