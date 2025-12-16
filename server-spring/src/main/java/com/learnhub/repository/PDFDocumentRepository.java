package com.learnhub.repository;

import com.learnhub.model.PDFDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PDFDocumentRepository extends JpaRepository<PDFDocument, Long> {

    Optional<PDFDocument> findByFilename(String filename);
}
