package com.nevgiu.hrai.evaluation;

import com.nevgiu.hrai.evaluation.dto.EvaluationRequest;
import com.nevgiu.hrai.evaluation.dto.EvaluationResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/evaluations")
public class EvaluationController {

    private final CvEvaluationService cvEvaluationService;

    public EvaluationController(CvEvaluationService cvEvaluationService) {
        this.cvEvaluationService = cvEvaluationService;
    }

    @PostMapping
    public EvaluationResponse evaluate(@Valid @RequestBody EvaluationRequest request) {
        return cvEvaluationService.evaluateCandidate(request);
    }
}
