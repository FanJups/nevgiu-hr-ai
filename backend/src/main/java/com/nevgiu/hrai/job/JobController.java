package com.nevgiu.hrai.job;

import com.nevgiu.hrai.job.dto.ApproveJobRequest;
import com.nevgiu.hrai.job.dto.JobGenerationRequest;
import com.nevgiu.hrai.job.dto.JobGenerationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // for dev; restrict in prod
public class JobController {

    private final JobGenerationService jobGenerationService;
    private final JobRepository jobRepository;

    @PostMapping("/generate")
    public JobGenerationResponse generate(@RequestBody JobGenerationRequest request) {
        return jobGenerationService.generateJobOffer(request);
    }

    @PostMapping("/approve")
    public Job approve(@RequestBody ApproveJobRequest request) {
        return jobGenerationService.approveJob(request);
    }

    @GetMapping
    public Iterable<Job> findAll() {
        return jobRepository.findAll();
    }

    @GetMapping("/{id}")
    public Job findById(@PathVariable Long id) {
        return jobRepository.findById(id).orElseThrow();
    }
}
