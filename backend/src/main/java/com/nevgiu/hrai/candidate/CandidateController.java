package com.nevgiu.hrai.candidate;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/candidates")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CandidateController {

    private final CandidateRepository candidateRepository;

    @GetMapping
    public Iterable<Candidate> findAll() {
        return candidateRepository.findAll();
    }

    @PostMapping
    public Candidate create(@RequestBody Candidate candidate) {
        return candidateRepository.save(candidate);
    }
}
