package com.nevgiu.hrai.candidate;

import com.nevgiu.hrai.candidate.ingestion.CvIngestionService;
import com.nevgiu.hrai.candidate.ingestion.dto.CvArchiveImportResult;
import com.nevgiu.hrai.candidate.ingestion.dto.CvImportResult;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/candidates")
@CrossOrigin(origins = "*")
public class CandidateController {

    private final CandidateRepository candidateRepository;
    private final CvIngestionService cvIngestionService;

    public CandidateController(CandidateRepository candidateRepository, CvIngestionService cvIngestionService) {
        this.candidateRepository = candidateRepository;
        this.cvIngestionService = cvIngestionService;
    }

    @GetMapping
    public Iterable<Candidate> findAll() {
        return candidateRepository.findAll();
    }

    @PostMapping
    public Candidate create(@RequestBody Candidate candidate) {
        return candidateRepository.save(candidate);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CvImportResult> importPdf(@RequestPart("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cvIngestionService.importPdf(file));
    }

    @PostMapping(value = "/import/archive", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CvArchiveImportResult importArchive(@RequestPart("file") MultipartFile file) {
        return cvIngestionService.importArchive(file);
    }

    @PostMapping("/import/initial")
    public CvArchiveImportResult importInitialArchive() {
        return cvIngestionService.importInitialArchive();
    }
}
