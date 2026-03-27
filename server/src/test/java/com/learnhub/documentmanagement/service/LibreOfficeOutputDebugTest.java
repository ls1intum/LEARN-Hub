package com.learnhub.documentmanagement.service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;

class LibreOfficeOutputDebugTest {

    static boolean libreOfficeAvailable() {
        try {
            Process p = new ProcessBuilder(
                "/Applications/LibreOffice.app/Contents/MacOS/soffice", "--version")
                .start();
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Test
    @EnabledIf("libreOfficeAvailable")
    void inspectLibreOfficeDocx() throws Exception {
        LibreOfficeConversionService conv = new LibreOfficeConversionService();
        // Set path via reflection since @Value won't work in unit test
        var field = LibreOfficeConversionService.class.getDeclaredField("libreofficePath");
        field.setAccessible(true);
        field.set(conv, "/Applications/LibreOffice.app/Contents/MacOS/soffice");
        var timeoutField = LibreOfficeConversionService.class.getDeclaredField("timeoutSeconds");
        timeoutField.setAccessible(true);
        timeoutField.set(conv, 60);

        String html = "<html><head><style>@page{size:A4 landscape;}</style></head>"
            + "<body><h1>Test</h1><p>Hello world</p></body></html>";
        byte[] docx = conv.convertHtmlToDocx(html.getBytes(StandardCharsets.UTF_8));

        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(docx))) {
            CTBody body = doc.getDocument().getBody();
            CTSectPr sectPr = body.getSectPr();
            if (sectPr != null) {
                System.out.println("=== Body SectPr ===");
                if (sectPr.isSetPgSz()) {
                    System.out.println("PgSz W=" + sectPr.getPgSz().getW()
                        + " H=" + sectPr.getPgSz().getH()
                        + " Orient=" + sectPr.getPgSz().getOrient());
                } else {
                    System.out.println("No PgSz set");
                }
                System.out.println("Header refs: " + sectPr.getHeaderReferenceList().size());
                System.out.println("Footer refs: " + sectPr.getFooterReferenceList().size());
            } else {
                System.out.println("No body SectPr");
            }
            System.out.println("Headers: " + doc.getHeaderList().size());
            System.out.println("Footers: " + doc.getFooterList().size());
            System.out.println("Paragraphs: " + doc.getParagraphs().size());

            // Check for inline sectPr in paragraphs
            int inlineSectPr = 0;
            for (var para : body.getPList()) {
                if (para.getPPr() != null && para.getPPr().getSectPr() != null) {
                    inlineSectPr++;
                    CTSectPr isp = para.getPPr().getSectPr();
                    System.out.println("Inline SectPr: W=" + 
                        (isp.isSetPgSz() ? isp.getPgSz().getW() : "none") +
                        " H=" + (isp.isSetPgSz() ? isp.getPgSz().getH() : "none"));
                }
            }
            System.out.println("Inline sectPr count: " + inlineSectPr);
        }
    }
}
