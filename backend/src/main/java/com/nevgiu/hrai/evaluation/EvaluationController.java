package com.nevgiu.hrai.evaluation;

import com.nevgiu.hrai.evaluation.dto.EvaluationRequest;
import com.nevgiu.hrai.evaluation.dto.EvaluationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/evaluations")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class EvaluationController {

    private final CvEvaluationService cvEvaluationService;

    @PostMapping
    public EvaluationResponse evaluate(@RequestBody EvaluationRequest request) {
        return cvEvaluationService.evaluateCandidate(request);
    }
}
