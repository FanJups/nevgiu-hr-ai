package com.nevgiu.hrai.job;

import com.nevgiu.hrai.job.dto.ApproveJobRequest;
import com.nevgiu.hrai.job.dto.GeneratedJobOffer;
import com.nevgiu.hrai.job.dto.JobGenerationRequest;
import com.nevgiu.hrai.job.dto.JobGenerationResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobGenerationServiceTest {

    @Mock
    private ChatClient chatClient;

    @Mock
    private JobRepository jobRepository;

    @InjectMocks
    private JobGenerationService service;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void buildUserPrompt_includesAllFields_orNA() {

        var service = new JobGenerationService(chatClient, objectMapper, jobRepository);

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
    void parseModelResponse_parsesValidJson() {
        JobRepository dummyRepo = null;
        ChatClient chatClient = null;

        var service = new JobGenerationService(chatClient, new ObjectMapper(), dummyRepo);

        String json = ""
                + "{"
                + "  \"jobOffer\": {"
                + "    \"inferredTitle\": \"Senior Backend Developer\","
                + "    \"level\": \"Senior\","
                + "    \"summary\": \"We are looking for a Senior Backend Developer...\","
                + "    \"responsibilities\": [\"Build backend services\", \"Collaborate with team\"],"
                + "    \"requiredQualifications\": [\"5+ years experience\", \"Strong Python skills\"],"
                + "    \"preferredQualifications\": [\"Experience with AWS\", \"Docker\"],"
                + "    \"softSkills\": [\"Teamwork\", \"Communication\"],"
                + "    \"benefits\": [\"Health insurance\", \"Remote work\"],"
                + "    \"employmentType\": \"FULL_TIME\","
                + "    \"location\": \"Lisbon\","
                + "    \"salaryRange\": \"Competitive\","
                + "    \"tone\": \"inclusive\""
                + "  },"
                + "  \"missingInfo\": [\"department\"],"
                + "  \"suggestions\": [\"Consider adding salary range details\"]"
                + "}";

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
        ChatClient chatClient = null;

        var service = new JobGenerationService(chatClient, new ObjectMapper(), dummyRepo);

        JobGenerationResponse res = service.parseModelResponse("NOT JSON");

        assertThat(res.jobOffer().inferredTitle()).isEqualTo("Unknown Title");
        assertThat(res.missingInfo()).contains("parseError");
        assertThat(res.suggestions()).isNotEmpty();
    }

    @Test
    void approveJob_savesJobAndReturnsIt() {

        var service = new JobGenerationService(chatClient, objectMapper, jobRepository);

        JobGenerationRequest originalReq = new JobGenerationRequest(
                "Senior backend dev",
                "Engineering",
                "Lisbon",
                "FULL_TIME",
                "60k-80k",
                "inclusive"
        );

        GeneratedJobOffer offer = new GeneratedJobOffer(
                "Senior Backend Developer",
                "Senior",
                "We are looking for a Senior Backend Developer...",
                List.of("Do X", "Do Y"),
                List.of("Req1"),
                List.of("Pref1"),
                List.of("Soft1"),
                List.of("Benefit1"),
                "FULL_TIME",
                "Lisbon",
                "60k-80k",
                "inclusive"
        );

        ApproveJobRequest approveReq = new ApproveJobRequest(originalReq, offer);

        Job saved = new Job();
        saved.setId(123L);
        saved.setTitle("Senior Backend Developer");
        saved.setDepartment("Engineering");
        when(jobRepository.save(any(Job.class))).thenReturn(saved);

        Job result = service.approveJob(approveReq);

        assertThat(result.getId()).isEqualTo(123L);
        assertThat(result.getTitle()).isEqualTo("Senior Backend Developer");
        assertThat(result.getDepartment()).isEqualTo("Engineering");

        verify(jobRepository, times(1)).save(any(Job.class));
    }
}
