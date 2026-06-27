package com.learnhub.documentmanagement.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.learnhub.activitymanagement.entity.enums.DocumentType;
import com.learnhub.documentmanagement.entity.PDFDocument;
import com.learnhub.documentmanagement.service.PDFService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

class DocumentsControllerTest {

	private DocumentsController controller;
	private PDFService pdfService;

	@BeforeEach
	void setUp() {
		controller = new DocumentsController();
		pdfService = mock(PDFService.class);
		ReflectionTestUtils.setField(controller, "pdfService", pdfService);
	}

	private PDFDocument document(UUID id, DocumentType type) {
		PDFDocument doc = new PDFDocument();
		doc.setId(id);
		doc.setType(type);
		doc.setFilename("activity.pdf");
		doc.setFileSize(2048L);
		doc.setConfidenceScore("0.910");
		doc.setExtractionQuality("high");
		doc.setCreatedAt(LocalDateTime.now());
		return doc;
	}

	private Authentication admin() {
		return new UsernamePasswordAuthenticationToken("admin", null,
				List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
	}

	private Authentication teacher() {
		return new UsernamePasswordAuthenticationToken("teacher", null,
				List.of(new SimpleGrantedAuthority("ROLE_TEACHER")));
	}

	// ─── info ───────────────────────────────────────────────────────

	@Test
	void getDocumentInfoReturns200ForNonSourceDocument() {
		UUID id = UUID.randomUUID();
		when(pdfService.getPdfDocument(id)).thenReturn(document(id, null));

		ResponseEntity<?> response = controller.getDocumentInfo(id, teacher());

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@Test
	void getDocumentInfoReturns404WhenMissing() {
		UUID id = UUID.randomUUID();
		when(pdfService.getPdfDocument(id)).thenThrow(new RuntimeException("not found"));

		ResponseEntity<?> response = controller.getDocumentInfo(id, teacher());

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void getDocumentInfoAllowsAdminForSourcePdf() {
		UUID id = UUID.randomUUID();
		when(pdfService.getPdfDocument(id)).thenReturn(document(id, DocumentType.SOURCE_PDF));

		ResponseEntity<?> response = controller.getDocumentInfo(id, admin());

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@Test
	void getDocumentInfoDeniesNonAdminForSourcePdf() {
		UUID id = UUID.randomUUID();
		when(pdfService.getPdfDocument(id)).thenReturn(document(id, DocumentType.SOURCE_PDF));

		assertThatThrownBy(() -> controller.getDocumentInfo(id, teacher())).isInstanceOf(AccessDeniedException.class);
	}

	@Test
	void getDocumentInfoDeniesAnonymousForSourcePdf() {
		UUID id = UUID.randomUUID();
		when(pdfService.getPdfDocument(id)).thenReturn(document(id, DocumentType.SOURCE_PDF));

		assertThatThrownBy(() -> controller.getDocumentInfo(id, null)).isInstanceOf(AccessDeniedException.class);
	}

	// ─── download ───────────────────────────────────────────────────

	@Test
	void downloadDocumentReturnsPdfBytesForNonSource() throws Exception {
		UUID id = UUID.randomUUID();
		when(pdfService.getPdfDocument(id)).thenReturn(document(id, null));
		when(pdfService.getPdfContent(id)).thenReturn(new byte[]{0x25, 0x50, 0x44, 0x46});

		ResponseEntity<?> response = controller.downloadDocument(id, teacher());

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
		assertThat((byte[]) response.getBody()).hasSize(4);
	}

	@Test
	void downloadDocumentAllowsAdminForSourcePdf() throws Exception {
		UUID id = UUID.randomUUID();
		when(pdfService.getPdfDocument(id)).thenReturn(document(id, DocumentType.SOURCE_PDF));
		when(pdfService.getPdfContent(id)).thenReturn(new byte[]{0x25, 0x50, 0x44, 0x46});

		ResponseEntity<?> response = controller.downloadDocument(id, admin());

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@Test
	void downloadDocumentDeniesNonAdminForSourcePdf() {
		UUID id = UUID.randomUUID();
		when(pdfService.getPdfDocument(id)).thenReturn(document(id, DocumentType.SOURCE_PDF));

		assertThatThrownBy(() -> controller.downloadDocument(id, teacher())).isInstanceOf(AccessDeniedException.class);
	}

	@Test
	void downloadDocumentReturns404WhenMissing() throws Exception {
		UUID id = UUID.randomUUID();
		when(pdfService.getPdfDocument(id)).thenThrow(new RuntimeException("not found"));

		ResponseEntity<?> response = controller.downloadDocument(id, teacher());

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}
}
