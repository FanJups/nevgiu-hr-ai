package com.nevgiu.hrai.candidate.ingestion;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class PdfCvTextExtractorTest {

    private final PdfCvTextExtractor extractor = new PdfCvTextExtractor();

    @Test
    void extractsTextFromPdf() throws Exception {
        byte[] pdf;
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.newLineAtOffset(50, 700);
                content.showText("Ada Lovelace software engineer ada@example.com");
                content.endText();
            }
            document.save(output);
            pdf = output.toByteArray();
        }

        assertThat(extractor.extract(pdf)).contains("Ada Lovelace", "ada@example.com");
    }
}
