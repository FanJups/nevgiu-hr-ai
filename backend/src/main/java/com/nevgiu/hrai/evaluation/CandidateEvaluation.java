package com.nevgiu.hrai.evaluation;

import com.nevgiu.hrai.candidate.Candidate;
import com.nevgiu.hrai.job.Job;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "candidate_evaluations")
public class CandidateEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Candidate candidate;

    @ManyToOne(optional = false)
    private Job job;

    // Core fit
    private int skillsMatchScore;          // 0–100
    private int experienceRelevanceScore;  // 0–10
    private int educationFitScore;         // 0–10

    // Quality
    private int achievementImpactScore;    // 0–10
    private int keywordDensityScore;       // 0–100

    // Risk / confidence
    private int employmentGapScore;        // 0–10 (10 = no gaps)
    private int readabilityScore;          // 0–10
    private int aiConfidenceScore;         // 0–100

    // Composite
    private int overallFitScore;           // 0–100

    @Lob
    private String aiExplanation;

    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Candidate getCandidate() { return candidate; }
    public void setCandidate(Candidate candidate) { this.candidate = candidate; }

    public Job getJob() { return job; }
    public void setJob(Job job) { this.job = job; }

    public int getSkillsMatchScore() { return skillsMatchScore; }
    public void setSkillsMatchScore(int skillsMatchScore) { this.skillsMatchScore = skillsMatchScore; }

    public int getExperienceRelevanceScore() { return experienceRelevanceScore; }
    public void setExperienceRelevanceScore(int experienceRelevanceScore) { this.experienceRelevanceScore = experienceRelevanceScore; }

    public int getEducationFitScore() { return educationFitScore; }
    public void setEducationFitScore(int educationFitScore) { this.educationFitScore = educationFitScore; }

    public int getAchievementImpactScore() { return achievementImpactScore; }
    public void setAchievementImpactScore(int achievementImpactScore) { this.achievementImpactScore = achievementImpactScore; }

    public int getKeywordDensityScore() { return keywordDensityScore; }
    public void setKeywordDensityScore(int keywordDensityScore) { this.keywordDensityScore = keywordDensityScore; }

    public int getEmploymentGapScore() { return employmentGapScore; }
    public void setEmploymentGapScore(int employmentGapScore) { this.employmentGapScore = employmentGapScore; }

    public int getReadabilityScore() { return readabilityScore; }
    public void setReadabilityScore(int readabilityScore) { this.readabilityScore = readabilityScore; }

    public int getAiConfidenceScore() { return aiConfidenceScore; }
    public void setAiConfidenceScore(int aiConfidenceScore) { this.aiConfidenceScore = aiConfidenceScore; }

    public int getOverallFitScore() { return overallFitScore; }
    public void setOverallFitScore(int overallFitScore) { this.overallFitScore = overallFitScore; }

    public String getAiExplanation() { return aiExplanation; }
    public void setAiExplanation(String aiExplanation) { this.aiExplanation = aiExplanation; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
