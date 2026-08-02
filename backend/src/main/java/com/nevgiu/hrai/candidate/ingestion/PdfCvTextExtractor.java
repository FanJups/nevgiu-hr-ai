package com.nevgiu.hrai.candidate.ingestion;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class PdfCvTextExtractor implements CvTextExtractor {

    @Override
    public String extract(byte[] content) throws IOException {
        try (PDDocument document = Loader.loadPDF(content)) {
            if (document.isEncrypted()) {
                throw new IOException("Encrypted PDFs are not supported");
            }
            return new PDFTextStripper().getText(document).trim();
        }
    }
}
