package com.nevgiu.hrai.candidate.ingestion;

import com.nevgiu.hrai.candidate.Candidate;
import com.nevgiu.hrai.candidate.CandidateRepository;
import com.nevgiu.hrai.candidate.CvDocument;
import com.nevgiu.hrai.candidate.CvDocumentRepository;
import com.nevgiu.hrai.candidate.CvDocumentSource;
import com.nevgiu.hrai.candidate.CvIngestionStatus;
import com.nevgiu.hrai.candidate.ingestion.dto.CvArchiveImportResult;
import com.nevgiu.hrai.candidate.ingestion.dto.CvImportResult;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class CvIngestionService {

    private static final String PDF_CONTENT_TYPE = "application/pdf";
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", Pattern.CASE_INSENSITIVE);

    private final CandidateRepository candidateRepository;
    private final CvDocumentRepository documentRepository;
    private final CvTextExtractor textExtractor;
    private final CvIngestionProperties properties;
    private final ResourceLoader resourceLoader;

    public CvIngestionService(
            CandidateRepository candidateRepository,
            CvDocumentRepository documentRepository,
            CvTextExtractor textExtractor,
            CvIngestionProperties properties,
            ResourceLoader resourceLoader
    ) {
        this.candidateRepository = candidateRepository;
        this.documentRepository = documentRepository;
        this.textExtractor = textExtractor;
        this.properties = properties;
        this.resourceLoader = resourceLoader;
    }

    @Transactional
    public CvImportResult importPdf(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CvIngestionException(HttpStatus.BAD_REQUEST, "A non-empty PDF file is required");
        }
        try {
            return importPdf(file.getOriginalFilename(), file.getContentType(), file.getBytes(), CvDocumentSource.USER_UPLOAD);
        } catch (IOException e) {
            throw new CvIngestionException(HttpStatus.BAD_REQUEST, "Unable to read the uploaded PDF");
        }
    }

    @Transactional
    public CvArchiveImportResult importArchive(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CvIngestionException(HttpStatus.BAD_REQUEST, "A non-empty ZIP file is required");
        }
        validateArchiveName(file.getOriginalFilename());
        try {
            return importArchive(file.getInputStream(), file.getSize(), CvDocumentSource.USER_UPLOAD);
        } catch (IOException e) {
            throw new CvIngestionException(HttpStatus.BAD_REQUEST, "Unable to read the uploaded ZIP archive");
        }
    }

    @Transactional
    public CvArchiveImportResult importInitialArchive() {
        if (!properties.initialImportEnabled()) {
            throw new CvIngestionException(HttpStatus.FORBIDDEN, "Initial CV import is disabled");
        }
        Resource resource = resourceLoader.getResource(properties.initialResource());
        if (!resource.exists()) {
            throw new CvIngestionException(HttpStatus.NOT_FOUND, "Initial CV archive was not found");
        }
        try (InputStream input = resource.getInputStream()) {
            return importArchive(input, resource.contentLength(), CvDocumentSource.INITIAL_DATA);
        } catch (IOException e) {
            throw new CvIngestionException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to read the initial CV archive");
        }
    }

    CvArchiveImportResult importArchive(InputStream input, long archiveSize, CvDocumentSource source) throws IOException {
        if (archiveSize > properties.maxArchiveSize()) {
            throw new CvIngestionException(HttpStatus.PAYLOAD_TOO_LARGE, "ZIP archive exceeds the configured size limit");
        }

        List<CvImportResult> results = new ArrayList<>();
        long totalExpanded = 0;
        int fileCount = 0;

        try (ZipInputStream zip = new ZipInputStream(input, StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    zip.closeEntry();
                    continue;
                }
                fileCount++;
                if (fileCount > properties.maxArchiveEntries()) {
                    throw new CvIngestionException(HttpStatus.PAYLOAD_TOO_LARGE, "ZIP archive contains too many files");
                }

                String entryName = validateEntryName(entry.getName());
                if (!entryName.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
                    results.add(result(entryName, CvIngestionStatus.SKIPPED, List.of("Unsupported archive entry; only PDF files are imported")));
                    zip.closeEntry();
                    continue;
                }

                try {
                    byte[] content = readEntry(zip, properties.maxPdfSize());
                    totalExpanded += content.length;
                    if (totalExpanded > properties.maxExpandedSize()) {
                        throw new CvIngestionException(HttpStatus.PAYLOAD_TOO_LARGE, "ZIP archive expands beyond the configured limit");
                    }
                    if (entry.getCompressedSize() > 0
                            && ((double) content.length / entry.getCompressedSize()) > properties.maxCompressionRatio()) {
                        throw new CvIngestionException(HttpStatus.PAYLOAD_TOO_LARGE, "ZIP entry has an unsafe compression ratio: " + entryName);
                    }
                    results.add(importPdf(entryName, PDF_CONTENT_TYPE, content, source));
                } catch (CvIngestionException e) {
                    if (e.getStatus() == HttpStatus.PAYLOAD_TOO_LARGE) {
                        throw e;
                    }
                    results.add(result(entryName, CvIngestionStatus.FAILED, List.of(e.getMessage())));
                } catch (RuntimeException e) {
                    results.add(result(entryName, CvIngestionStatus.FAILED, List.of("Unexpected ingestion failure")));
                } finally {
                    zip.closeEntry();
                }
            }
        }

        return summarize(results);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CvImportResult importPdf(String filename, String contentType, byte[] content, CvDocumentSource source) {
        String safeFilename = validatePdf(filename, contentType, content);
        String hash = sha256(content);
        Optional<CvDocument> existing = documentRepository.findBySha256(hash);
        if (existing.isPresent()) {
            CvDocument document = existing.get();
            Long candidateId = document.getCandidate() == null ? null : document.getCandidate().getId();
            return new CvImportResult(candidateId, document.getId(), safeFilename, CvIngestionStatus.DUPLICATE,
                    PDF_CONTENT_TYPE, document.getTextLength(), List.of("Document content was already imported"));
        }

        CvDocument document = new CvDocument();
        document.setOriginalFilename(safeFilename);
        document.setContentType(PDF_CONTENT_TYPE);
        document.setFileSize(content.length);
        document.setSha256(hash);
        document.setSource(source);

        try {
            String text = textExtractor.extract(content);
            document.setExtractedText(text);
            document.setTextLength(text.length());

            List<String> warnings = new ArrayList<>();
            if (text.length() < properties.minimumTextLength()) {
                document.setStatus(CvIngestionStatus.NEEDS_REVIEW);
                warnings.add("Little or no text was extracted; OCR or manual review may be required");
                CvDocument saved = documentRepository.save(document);
                return new CvImportResult(null, saved.getId(), safeFilename, saved.getStatus(), PDF_CONTENT_TYPE,
                        saved.getTextLength(), warnings);
            }

            Candidate candidate = new Candidate();
            candidate.setName(deriveName(safeFilename));
            candidate.setEmail(extractEmail(text));
            candidate.setCvText(text);
            candidate = candidateRepository.save(candidate);

            document.setCandidate(candidate);
            document.setStatus(CvIngestionStatus.IMPORTED);
            CvDocument saved = documentRepository.save(document);
            return new CvImportResult(candidate.getId(), saved.getId(), safeFilename, saved.getStatus(), PDF_CONTENT_TYPE,
                    saved.getTextLength(), warnings);
        } catch (IOException e) {
            document.setStatus(CvIngestionStatus.FAILED);
            document.setIngestionError(e.getMessage());
            CvDocument saved = documentRepository.save(document);
            return new CvImportResult(null, saved.getId(), safeFilename, saved.getStatus(), PDF_CONTENT_TYPE, 0,
                    List.of("PDF text extraction failed"));
        }
    }

    private String validatePdf(String filename, String contentType, byte[] content) {
        String safeFilename = leafFilename(filename == null ? "cv.pdf" : filename);
        if (!safeFilename.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            throw new CvIngestionException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Only PDF files are supported");
        }
        if (content.length == 0) {
            throw new CvIngestionException(HttpStatus.BAD_REQUEST, "The PDF file is empty");
        }
        if (content.length > properties.maxPdfSize()) {
            throw new CvIngestionException(HttpStatus.PAYLOAD_TOO_LARGE, "PDF exceeds the configured size limit");
        }
        if (content.length < 5 || content[0] != '%' || content[1] != 'P' || content[2] != 'D' || content[3] != 'F' || content[4] != '-') {
            throw new CvIngestionException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "File content is not a PDF");
        }
        if (contentType != null && !contentType.isBlank()
                && !PDF_CONTENT_TYPE.equalsIgnoreCase(contentType)
                && !"application/octet-stream".equalsIgnoreCase(contentType)) {
            throw new CvIngestionException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Uploaded content type is not PDF");
        }
        return safeFilename;
    }

    private void validateArchiveName(String filename) {
        if (filename == null || !filename.toLowerCase(Locale.ROOT).endsWith(".zip")) {
            throw new CvIngestionException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Only ZIP archives are supported");
        }
    }

    private String validateEntryName(String name) {
        if (name == null || name.isBlank()) {
            throw new CvIngestionException(HttpStatus.BAD_REQUEST, "ZIP archive contains an unnamed entry");
        }
        String normalized = name.replace('\\', '/');
        if (normalized.startsWith("/") || normalized.matches("^[A-Za-z]:/.*")) {
            throw new CvIngestionException(HttpStatus.BAD_REQUEST, "ZIP archive contains an absolute path");
        }
        for (String segment : normalized.split("/")) {
            if ("..".equals(segment)) {
                throw new CvIngestionException(HttpStatus.BAD_REQUEST, "ZIP archive contains path traversal");
            }
        }
        return normalized;
    }

    private byte[] readEntry(InputStream input, long limit) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        long total = 0;
        int read;
        while ((read = input.read(buffer)) != -1) {
            total += read;
            if (total > limit) {
                throw new CvIngestionException(HttpStatus.PAYLOAD_TOO_LARGE, "PDF entry exceeds the configured size limit");
            }
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    private String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    private String deriveName(String filename) {
        String leaf = leafFilename(filename);
        String base = leaf.substring(0, leaf.length() - 4)
                .replaceAll("(?i)([-_ ]?(resume|curriculum[-_ ]?vitae|cv))+$", "")
                .replace('-', ' ')
                .replace('_', ' ')
                .replaceAll("\\s+", " ")
                .trim();
        if (base.isBlank()) {
            return "Unknown Candidate";
        }
        StringBuilder result = new StringBuilder();
        for (String part : base.split(" ")) {
            if (!result.isEmpty()) result.append(' ');
            result.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return result.toString();
    }

    private String extractEmail(String text) {
        Matcher matcher = EMAIL_PATTERN.matcher(text);
        return matcher.find() ? matcher.group() : null;
    }

    private String leafFilename(String filename) {
        String normalized = filename.replace('\\', '/');
        int separator = normalized.lastIndexOf('/');
        return separator >= 0 ? normalized.substring(separator + 1) : normalized;
    }

    private CvImportResult result(String filename, CvIngestionStatus status, List<String> warnings) {
        return new CvImportResult(null, null, filename, status, null, 0, warnings);
    }

    private CvArchiveImportResult summarize(List<CvImportResult> results) {
        int imported = 0, duplicates = 0, needsReview = 0, skipped = 0, failed = 0;
        for (CvImportResult result : results) {
            switch (result.status()) {
                case IMPORTED -> imported++;
                case DUPLICATE -> duplicates++;
                case NEEDS_REVIEW -> needsReview++;
                case SKIPPED -> skipped++;
                case FAILED -> failed++;
            }
        }
        return new CvArchiveImportResult(results.size(), imported, duplicates, needsReview, skipped, failed, List.copyOf(results));
    }
}
