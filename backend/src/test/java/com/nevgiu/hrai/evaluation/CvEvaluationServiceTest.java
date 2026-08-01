package com.nevgiu.hrai.evaluation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nevgiu.hrai.evaluation.dto.AiEvaluationResult;
import com.nevgiu.hrai.evaluation.dto.EvaluationWeights;
import com.nevgiu.hrai.evaluation.dto.EvaluationRequest;
import com.nevgiu.hrai.candidate.Candidate;
import com.nevgiu.hrai.candidate.CandidateRepository;
import com.nevgiu.hrai.job.Job;
import com.nevgiu.hrai.job.JobRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CvEvaluationServiceTest {

    @Test
    void parseModelResponse_parsesValidJson() {
        CvEvaluationService service = new CvEvaluationService(
                null, new ObjectMapper(), null, null, null
        );

        String json = ""
                + "{"
                + "  \"scores\": {"
                + "    \"skillsMatchScore\": 80,"
                + "    \"experienceRelevanceScore\": 8,"
                + "    \"educationFitScore\": 7,"
                + "    \"achievementImpactScore\": 6,"
                + "    \"keywordDensityScore\": 70,"
                + "    \"employmentGapScore\": 9,"
                + "    \"readabilityScore\": 8,"
                + "    \"aiConfidenceScore\": 90"
                + "  },"
                + "  \"explanation\": \"Strong skills and experience match.\""
                + "}";

        AiEvaluationResult result = service.parseModelResponse(json);

        assertThat(result.scores().skillsMatchScore()).isEqualTo(80);
        assertThat(result.scores().experienceRelevanceScore()).isEqualTo(8);
        assertThat(result.explanation()).contains("Strong skills");
    }

    @Test
    void parseModelResponse_handlesInvalidJson() {
        CvEvaluationService service = new CvEvaluationService(
                null, new ObjectMapper(), null, null, null
        );

        assertThatThrownBy(() -> service.parseModelResponse("NOT JSON"))
                .isInstanceOf(EvaluationException.class)
                .hasMessage("AI evaluation returned an invalid response");
    }

    @Test
    void computeComposite_usesWeightsAndNormalization() {
        CvEvaluationService service = new CvEvaluationService(
                null, new ObjectMapper(), null, null, null
        );

        AiEvaluationResult.Scores scores = new AiEvaluationResult.Scores(
                80, // skills
                8,  // exp
                7,  // edu
                6,  // achievement
                70, // keyword
                9,  // gap
                8,  // readability
                90  // confidence
        );

        EvaluationWeights weights = new EvaluationWeights(
                0.25, 0.15, 0.15, 0.15, 0.10, 0.10, 0.05, 0.05
        );

        int overall = service.computeComposite(scores, weights);

        assertThat(overall).isBetween(0, 100);
        assertThat(overall).isGreaterThan(70);
    }

    @Test
    void computeComposite_rejectsWeightsThatDoNotTotalOne() {
        CvEvaluationService service = new CvEvaluationService(
                null, new ObjectMapper(), null, null, null
        );

        AiEvaluationResult.Scores scores = new AiEvaluationResult.Scores(
                80, 8, 7, 6, 70, 9, 8, 90
        );
        EvaluationWeights invalid = new EvaluationWeights(
                0.25, 0.15, 0.15, 0.15, 0.10, 0.10, 0.05, 0.10
        );

        assertThatThrownBy(() -> service.computeComposite(scores, invalid))
                .isInstanceOf(EvaluationException.class)
                .hasMessage("Evaluation weights must total 1.0");
    }

    @Test
    void computeComposite_rejectsOutOfRangeScores() {
        CvEvaluationService service = new CvEvaluationService(
                null, new ObjectMapper(), null, null, null
        );

        AiEvaluationResult.Scores scores = new AiEvaluationResult.Scores(
                101, 8, 7, 6, 70, 9, 8, 90
        );

        assertThatThrownBy(() -> service.computeComposite(scores, service.defaultWeights()))
                .isInstanceOf(EvaluationException.class)
                .hasMessage("AI evaluation returned scores outside the allowed ranges");
    }

    @Test
    void evaluateCandidate_rejectsUnknownCandidateBeforeCallingProvider() {
        CandidateRepository candidates = mock(CandidateRepository.class);
        when(candidates.findById(99L)).thenReturn(Optional.empty());
        CvEvaluationService service = new CvEvaluationService(
                null, new ObjectMapper(), candidates, mock(JobRepository.class), null);

        assertThatThrownBy(() -> service.evaluateCandidate(new EvaluationRequest(99L, 1L, null)))
                .isInstanceOf(EvaluationException.class)
                .hasMessage("Candidate not found")
                .satisfies(error -> assertThat(((EvaluationException) error).getStatus().value()).isEqualTo(404));
    }

    @Test
    void evaluateCandidate_rejectsBlankCvBeforeCallingProvider() {
        Candidate candidate = new Candidate();
        candidate.setCvText("  ");
        CandidateRepository candidates = mock(CandidateRepository.class);
        JobRepository jobs = mock(JobRepository.class);
        when(candidates.findById(1L)).thenReturn(Optional.of(candidate));
        when(jobs.findById(2L)).thenReturn(Optional.of(new Job()));
        CvEvaluationService service = new CvEvaluationService(
                null, new ObjectMapper(), candidates, jobs, null);

        assertThatThrownBy(() -> service.evaluateCandidate(new EvaluationRequest(1L, 2L, null)))
                .isInstanceOf(EvaluationException.class)
                .hasMessage("Candidate has no extracted CV text")
                .satisfies(error -> assertThat(((EvaluationException) error).getStatus().value()).isEqualTo(422));
    }
}
