package com.learnhub.documentmanagement.repository;

import com.learnhub.documentmanagement.entity.PDFDocument;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PDFDocumentRepository extends JpaRepository<PDFDocument, UUID> {

	Optional<PDFDocument> findByFilename(String filename);
}
