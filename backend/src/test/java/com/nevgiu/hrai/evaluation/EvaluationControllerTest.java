package com.nevgiu.hrai.evaluation;

import com.nevgiu.hrai.web.ApiExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EvaluationController.class)
@ContextConfiguration(classes = {EvaluationController.class, ApiExceptionHandler.class})
class EvaluationControllerTest {

    @Autowired MockMvc mvc;
    @MockitoBean CvEvaluationService service;

    @Test
    void rejectsMissingCandidateAndJobIdsBeforeCallingService() throws Exception {
        mvc.perform(post("/api/evaluations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"weights\":null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.candidateId").exists())
                .andExpect(jsonPath("$.validationErrors.jobId").exists());
        verifyNoInteractions(service);
    }

    @Test
    void rejectsNegativeWeightsBeforeCallingService() throws Exception {
        mvc.perform(post("/api/evaluations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"candidateId":1,"jobId":2,"weights":{
                                  "skillsWeight":-0.1,"experienceWeight":0.1,"educationWeight":0.1,
                                  "achievementWeight":0.1,"qualityWeight":0.1,"gapWeight":0.1,
                                  "readabilityWeight":0.1,"confidenceWeight":0.4}}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors['weights.skillsWeight']").exists());
        verifyNoInteractions(service);
    }
}
