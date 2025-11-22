package com.nevgiu.hrai.evaluation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nevgiu.hrai.evaluation.dto.AiEvaluationResult;
import com.nevgiu.hrai.evaluation.dto.EvaluationWeights;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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

        AiEvaluationResult result = service.parseModelResponse("NOT JSON");

        assertThat(result.scores().skillsMatchScore()).isEqualTo(0);
        assertThat(result.explanation()).contains("Failed to parse");
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
}
