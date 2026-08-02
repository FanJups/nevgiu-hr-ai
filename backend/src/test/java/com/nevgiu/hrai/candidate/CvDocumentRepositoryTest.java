package com.nevgiu.hrai.candidate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = {
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class CvDocumentRepositoryTest {

    @Autowired CandidateRepository candidates;
    @Autowired CvDocumentRepository documents;

    @Test
    void persistsCandidateDocumentRelationshipAndLoadsItByHash() {
        Candidate candidate = new Candidate();
        candidate.setName("Ada Lovelace");
        candidate.setCvText("Analytical engine programmer");
        candidate = candidates.save(candidate);

        CvDocument document = document("a".repeat(64));
        document.setCandidate(candidate);
        documents.saveAndFlush(document);

        CvDocument loaded = documents.findBySha256(document.getSha256()).orElseThrow();
        assertThat(loaded.getCandidate().getId()).isEqualTo(candidate.getId());
        assertThat(loaded.getStatus()).isEqualTo(CvIngestionStatus.IMPORTED);
    }

    @Test
    void enforcesUniqueContentHash() {
        documents.saveAndFlush(document("b".repeat(64)));
        assertThatThrownBy(() -> documents.saveAndFlush(document("b".repeat(64))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private CvDocument document(String hash) {
        CvDocument document = new CvDocument();
        document.setOriginalFilename("candidate.pdf");
        document.setContentType("application/pdf");
        document.setFileSize(100);
        document.setSha256(hash);
        document.setSource(CvDocumentSource.USER_UPLOAD);
        document.setStatus(CvIngestionStatus.IMPORTED);
        document.setExtractedText("CV text");
        document.setTextLength(7);
        return document;
    }
}
