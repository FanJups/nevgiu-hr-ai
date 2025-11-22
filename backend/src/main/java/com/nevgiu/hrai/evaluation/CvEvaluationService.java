package com.nevgiu.hrai.evaluation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nevgiu.hrai.candidate.Candidate;
import com.nevgiu.hrai.candidate.CandidateRepository;
import com.nevgiu.hrai.evaluation.dto.AiEvaluationResult;
import com.nevgiu.hrai.evaluation.dto.CandidateEvaluationDto;
import com.nevgiu.hrai.evaluation.dto.EvaluationRequest;
import com.nevgiu.hrai.evaluation.dto.EvaluationResponse;
import com.nevgiu.hrai.evaluation.dto.EvaluationWeights;
import com.nevgiu.hrai.job.Job;
import com.nevgiu.hrai.job.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class CvEvaluationService {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final CandidateRepository candidateRepository;
    private final JobRepository jobRepository;
    private final CandidateEvaluationRepository evaluationRepository;

    public EvaluationResponse evaluateCandidate(EvaluationRequest request) {
        Candidate candidate = candidateRepository.findById(request.candidateId())
                .orElseThrow(() -> new IllegalArgumentException("Candidate not found"));
        Job job = jobRepository.findById(request.jobId())
                .orElseThrow(() -> new IllegalArgumentException("Job not found"));

        String systemPrompt = buildSystemPrompt();
        String userPrompt = buildUserPrompt(job, candidate);

        String modelResponse = chatClient
                .prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .content();

        AiEvaluationResult aiResult = parseModelResponse(modelResponse);

        EvaluationWeights weights = (request.weights() != null)
                ? request.weights()
                : defaultWeights();

        int overall = computeComposite(aiResult.scores(), weights);

        CandidateEvaluation entity = new CandidateEvaluation();
        entity.setCandidate(candidate);
        entity.setJob(job);
        entity.setSkillsMatchScore(aiResult.scores().skillsMatchScore());
        entity.setExperienceRelevanceScore(aiResult.scores().experienceRelevanceScore());
        entity.setEducationFitScore(aiResult.scores().educationFitScore());
        entity.setAchievementImpactScore(aiResult.scores().achievementImpactScore());
        entity.setKeywordDensityScore(aiResult.scores().keywordDensityScore());
        entity.setEmploymentGapScore(aiResult.scores().employmentGapScore());
        entity.setReadabilityScore(aiResult.scores().readabilityScore());
        entity.setAiConfidenceScore(aiResult.scores().aiConfidenceScore());
        entity.setOverallFitScore(overall);
        entity.setAiExplanation(aiResult.explanation());
        entity.setCreatedAt(Instant.now());

        evaluationRepository.save(entity);

        CandidateEvaluationDto dto = mapToDto(entity);

        return new EvaluationResponse(dto, entity.getAiExplanation());
    }

    String buildUserPrompt(Job job, Candidate candidate) {
        String jobText = job.getSummary() != null ? job.getSummary() : "";
        if (job.getResponsibilities() != null) {
            jobText += "\nResponsibilities:\n" + job.getResponsibilities();
        }
        if (job.getRequiredQualifications() != null) {
            jobText += "\nRequired qualifications:\n" + job.getRequiredQualifications();
        }

        return String.format(
                "JOB DESCRIPTION:%n%s%n%nCV:%n%s%n",
                jobText,
                candidate.getCvText()
        );
    }

    String buildSystemPrompt() {
        return ""
                + "You are an AI assistant that scores candidates for a job based purely on their CV and the job description.\n"
                + "Return an objective evaluation using exactly these metrics:\n"
                + "1. Skills Match Score: 0–100. Percentage overlap of required vs present skills (semantic matches allowed).\n"
                + "2. Experience Relevance Score: 0–10. Relevance of roles/industry, weighted by recency.\n"
                + "3. Education Fit Score: 0–10. Degree level and field + relevant certifications.\n"
                + "4. Achievement Impact Score: 0–10. Presence of quantified, impactful achievements.\n"
                + "5. Keyword Density Score: 0–100. Reasonable presence of job-specific terms without obvious keyword stuffing.\n"
                + "6. Employment Gap Score: 0–10 (10 = no gaps). Penalize unexplained gaps > 6 months.\n"
                + "7. Readability and Structure Score: 0–10. Clarity, structure, reasonable length.\n"
                + "8. Overall AI Confidence Score: 0–100. Confidence that the CV was parsed correctly.\n"
                + "Avoid any demographic or diversity-based scoring.\n"
                + "Return ONLY a single JSON object like:\n"
                + "{\n"
                + "  \"scores\": {\n"
                + "    \"skillsMatchScore\": 0,\n"
                + "    \"experienceRelevanceScore\": 0,\n"
                + "    \"educationFitScore\": 0,\n"
                + "    \"achievementImpactScore\": 0,\n"
                + "    \"keywordDensityScore\": 0,\n"
                + "    \"employmentGapScore\": 0,\n"
                + "    \"readabilityScore\": 0,\n"
                + "    \"aiConfidenceScore\": 0\n"
                + "  },\n"
                + "  \"explanation\": \"...\"\n"
                + "}\n";
    }

    AiEvaluationResult parseModelResponse(String json) {
        try {
            return objectMapper.readValue(json, AiEvaluationResult.class);
        } catch (IOException e) {
            AiEvaluationResult.Scores scores = new AiEvaluationResult.Scores(
                    0, 0, 0, 0, 0, 0, 0, 0
            );
            return new AiEvaluationResult(scores, "Failed to parse model response: " + e.getMessage());
        }
    }

    EvaluationWeights defaultWeights() {
        // Example: 40% skills/experience, 30% edu/achievements, 30% quality/risk
        return new EvaluationWeights(
                0.25, // skills
                0.15, // experience
                0.15, // education
                0.15, // achievement
                0.10, // keyword density
                0.10, // gap
                0.05, // readability
                0.05  // confidence
        );
    }

    int computeComposite(AiEvaluationResult.Scores s, EvaluationWeights w) {
        double skills = normalize100(s.skillsMatchScore()) * w.skillsWeight();
        double exp = normalize10(s.experienceRelevanceScore()) * w.experienceWeight();
        double edu = normalize10(s.educationFitScore()) * w.educationWeight();
        double ach = normalize10(s.achievementImpactScore()) * w.achievementWeight();
        double kw = normalize100(s.keywordDensityScore()) * w.qualityWeight();
        double gap = normalize10(s.employmentGapScore()) * w.gapWeight();
        double read = normalize10(s.readabilityScore()) * w.readabilityWeight();
        double conf = normalize100(s.aiConfidenceScore()) * w.confidenceWeight();

        double totalWeight = w.skillsWeight() + w.experienceWeight() + w.educationWeight()
                + w.achievementWeight() + w.qualityWeight() + w.gapWeight()
                + w.readabilityWeight() + w.confidenceWeight();

        double overall = (skills + exp + edu + ach + kw + gap + read + conf) / totalWeight;
        return (int) Math.round(overall);
    }

    double normalize10(int x) {
        return (x / 10.0) * 100.0;
    }

    double normalize100(int x) {
        return x;
    }

    CandidateEvaluationDto mapToDto(CandidateEvaluation e) {
        return new CandidateEvaluationDto(
                e.getId(),
                e.getCandidate().getId(),
                e.getJob().getId(),
                e.getSkillsMatchScore(),
                e.getExperienceRelevanceScore(),
                e.getEducationFitScore(),
                e.getAchievementImpactScore(),
                e.getKeywordDensityScore(),
                e.getEmploymentGapScore(),
                e.getReadabilityScore(),
                e.getAiConfidenceScore(),
                e.getOverallFitScore(),
                e.getAiExplanation(),
                e.getCreatedAt()
        );
    }
}
