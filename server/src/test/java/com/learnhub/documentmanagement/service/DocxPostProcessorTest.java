package com.learnhub.documentmanagement.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;

class DocxPostProcessorTest {

    @Test
    void processAddsHeaderFooterAndOrientation() throws Exception {
        // Create a minimal DOCX
        byte[] input;
        try (XWPFDocument doc = new XWPFDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            doc.createParagraph().createRun().setText("test content");
            doc.write(out);
            input = out.toByteArray();
        }

        DocxPostProcessor processor = new DocxPostProcessor();
        byte[] result = processor.process(input, true, "My Activity");

        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(result))) {
            // Check orientation
            CTSectPr sectPr = doc.getDocument().getBody().getSectPr();
            assertThat(sectPr).isNotNull();
            System.out.println("PgSz W=" + sectPr.getPgSz().getW() + " H=" + sectPr.getPgSz().getH());
            System.out.println("Orient=" + sectPr.getPgSz().getOrient());
            System.out.println("Header refs: " + sectPr.getHeaderReferenceList().size());
            System.out.println("Footer refs: " + sectPr.getFooterReferenceList().size());
            System.out.println("Headers: " + doc.getHeaderList().size());
            System.out.println("Footers: " + doc.getFooterList().size());
            if (!doc.getHeaderList().isEmpty()) {
                System.out.println("Header text: " + doc.getHeaderList().get(0).getText());
            }
            if (!doc.getFooterList().isEmpty()) {
                System.out.println("Footer text: " + doc.getFooterList().get(0).getText());
            }

            assertThat(sectPr.getPgSz().getOrient().toString()).isEqualTo("landscape");
            assertThat(doc.getHeaderList()).isNotEmpty();
            assertThat(doc.getFooterList()).isNotEmpty();
        }
    }
}
