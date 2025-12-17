package com.learnhub.service;

import com.learnhub.model.PDFDocument;
import com.learnhub.repository.PDFDocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

@Service
public class PDFService {

    @Autowired
    private PDFDocumentRepository pdfDocumentRepository;

    @Value("${pdf.storage.path:/app/data/pdfs}")
    private String pdfStoragePath;

    @Transactional
    public Long storePdf(byte[] pdfContent, String filename) throws IOException {
        // Ensure storage directory exists
        Path storagePath = Paths.get(pdfStoragePath);
        Files.createDirectories(storagePath);

        // Save PDF to filesystem
        Path filePath = storagePath.resolve(filename);
        Files.write(filePath, pdfContent);

        // Create database record
        PDFDocument document = new PDFDocument();
        document.setFilename(filename);
        document.setFilePath(filePath.toString());
        document.setFileSize((long) pdfContent.length);
        document.setExtractedFields("{}");
        document.setCreatedAt(LocalDateTime.now());

        document = pdfDocumentRepository.save(document);
        return document.getId();
    }

    public byte[] getPdfContent(Long documentId) throws IOException {
        PDFDocument document = pdfDocumentRepository.findById(documentId)
            .orElseThrow(() -> new RuntimeException("PDF document not found"));

        Path filePath = Paths.get(document.getFilePath());
        if (!Files.exists(filePath)) {
            throw new RuntimeException("PDF file not found on filesystem");
        }

        return Files.readAllBytes(filePath);
    }

    public PDFDocument getPdfDocument(Long documentId) {
        return pdfDocumentRepository.findById(documentId)
            .orElseThrow(() -> new RuntimeException("PDF document not found"));
    }
}
