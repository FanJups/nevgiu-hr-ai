package com.nevgiu.hrai.candidate.ingestion;

import com.nevgiu.hrai.candidate.Candidate;
import com.nevgiu.hrai.candidate.CandidateRepository;
import com.nevgiu.hrai.candidate.CvDocument;
import com.nevgiu.hrai.candidate.CvDocumentRepository;
import com.nevgiu.hrai.candidate.CvDocumentSource;
import com.nevgiu.hrai.candidate.CvIngestionStatus;
import com.nevgiu.hrai.candidate.ingestion.dto.CvArchiveImportResult;
import com.nevgiu.hrai.candidate.ingestion.dto.CvImportResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.http.HttpStatus;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CvIngestionServiceTest {

    private CandidateRepository candidateRepository;
    private CvDocumentRepository documentRepository;
    private CvIngestionService service;
    private final AtomicLong ids = new AtomicLong(1);

    @BeforeEach
    void setUp() {
        candidateRepository = mock(CandidateRepository.class);
        documentRepository = mock(CvDocumentRepository.class);
        CvTextExtractor extractor = content -> "Ada Lovelace\nada@example.com\nSoftware engineer with extensive analytical experience.";
        CvIngestionProperties properties = new CvIngestionProperties(
                1_000_000, 2_000_000, 10, 2_000_000, 100, 50,
                "classpath:intial/CVs.zip", true);
        service = new CvIngestionService(candidateRepository, documentRepository, extractor, properties, new DefaultResourceLoader());

        when(documentRepository.findBySha256(any())).thenReturn(Optional.empty());
        when(candidateRepository.save(any(Candidate.class))).thenAnswer(invocation -> {
            Candidate candidate = invocation.getArgument(0);
            candidate.setId(ids.getAndIncrement());
            return candidate;
        });
        when(documentRepository.save(any(CvDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void importsPdfAndCreatesCandidate() {
        CvImportResult result = service.importPdf(
                "ada-lovelace-resume.pdf", "application/pdf", minimalPdfBytes(), CvDocumentSource.USER_UPLOAD);

        assertThat(result.status()).isEqualTo(CvIngestionStatus.IMPORTED);
        assertThat(result.candidateId()).isNotNull();
        assertThat(result.textLength()).isGreaterThan(50);
    }

    @Test
    void rejectsNonPdfContent() {
        assertThatThrownBy(() -> service.importPdf(
                "candidate.pdf", "application/pdf", "not-pdf".getBytes(), CvDocumentSource.USER_UPLOAD))
                .isInstanceOf(CvIngestionException.class)
                .satisfies(error -> assertThat(((CvIngestionException) error).getStatus())
                        .isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE));
    }

    @Test
    void archiveContinuesPastUnsupportedEntry() throws Exception {
        byte[] archive = zip(
                new Entry("CVs/candidate.pdf", minimalPdfBytes()),
                new Entry("CVs/readme.txt", "ignore".getBytes())
        );

        CvArchiveImportResult result = service.importArchive(
                new ByteArrayInputStream(archive), archive.length, CvDocumentSource.USER_UPLOAD);

        assertThat(result.totalFiles()).isEqualTo(2);
        assertThat(result.imported()).isEqualTo(1);
        assertThat(result.skipped()).isEqualTo(1);
    }

    @Test
    void archiveRejectsPathTraversal() throws Exception {
        byte[] archive = zip(new Entry("../candidate.pdf", minimalPdfBytes()));

        assertThatThrownBy(() -> service.importArchive(
                new ByteArrayInputStream(archive), archive.length, CvDocumentSource.USER_UPLOAD))
                .isInstanceOf(CvIngestionException.class)
                .hasMessage("ZIP archive contains path traversal");
    }

    private byte[] minimalPdfBytes() {
        return "%PDF-1.4\n%%EOF".getBytes();
    }

    private byte[] zip(Entry... entries) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            for (Entry entry : entries) {
                zip.putNextEntry(new ZipEntry(entry.name()));
                zip.write(entry.content());
                zip.closeEntry();
            }
        }
        return output.toByteArray();
    }

    private record Entry(String name, byte[] content) {}
}
