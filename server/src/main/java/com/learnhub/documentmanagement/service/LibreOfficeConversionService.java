package com.learnhub.documentmanagement.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Converts HTML files to DOCX format using LibreOffice in headless mode.
 */
@Service
public class LibreOfficeConversionService {

	private static final Logger logger = LoggerFactory.getLogger(LibreOfficeConversionService.class);

	@Value("${learnhub.libreoffice.path:soffice}")
	private String libreofficePath;

	@Value("${learnhub.libreoffice.timeout-seconds:60}")
	private int timeoutSeconds;

	/**
	 * Convert HTML bytes to DOCX bytes using LibreOffice headless. LibreOffice
	 * opens HTML as a "Writer/Web" document which cannot export directly to DOCX,
	 * so this performs a two-step conversion: HTML → ODT → DOCX.
	 *
	 * @param htmlBytes
	 *            the HTML content as UTF-8 bytes
	 * @return the resulting DOCX content
	 * @throws IOException
	 *             if file I/O or the conversion process fails
	 */
	public byte[] convertHtmlToDocx(byte[] htmlBytes) throws IOException {
		Path tempDir = Files.createTempDirectory("libreoffice-convert-");
		String userProfileUri = "file://" + tempDir.resolve("profile-" + UUID.randomUUID()).toAbsolutePath();

		try {
			Path htmlFile = tempDir.resolve("input.html");
			Files.write(htmlFile, htmlBytes);

			// Step 1: HTML → ODT
			runLibreOffice(tempDir, userProfileUri, "odt", htmlFile);
			Path odtFile = tempDir.resolve("input.odt");
			if (!Files.exists(odtFile)) {
				throw new IOException("LibreOffice HTML-to-ODT conversion produced no output file");
			}

			// Step 2: ODT → DOCX
			runLibreOffice(tempDir, userProfileUri, "docx", odtFile);
			Path docxFile = tempDir.resolve("input.docx");
			if (!Files.exists(docxFile)) {
				throw new IOException("LibreOffice ODT-to-DOCX conversion produced no output file");
			}

			return Files.readAllBytes(docxFile);

		} finally {
			deleteRecursively(tempDir);
		}
	}

	private void runLibreOffice(Path outDir, String userProfileUri, String targetFormat, Path inputFile)
			throws IOException {
		ProcessBuilder pb = new ProcessBuilder(libreofficePath, "--headless", "--convert-to", targetFormat, "--outdir",
				outDir.toString(), "-env:UserInstallation=" + userProfileUri, inputFile.toString());
		pb.redirectErrorStream(true);

		try {
			Process process = pb.start();
			String output = new String(process.getInputStream().readAllBytes());

			boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
			if (!finished) {
				process.destroyForcibly();
				throw new IOException("LibreOffice conversion timed out after " + timeoutSeconds + " seconds");
			}

			int exitCode = process.exitValue();
			if (exitCode != 0) {
				logger.error("LibreOffice conversion failed (exit code {}): {}", exitCode, output);
				throw new IOException("LibreOffice conversion failed with exit code " + exitCode);
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException("LibreOffice conversion was interrupted", e);
		}
	}

	private void deleteRecursively(Path path) {
		try {
			if (Files.isDirectory(path)) {
				try (var entries = Files.list(path)) {
					entries.forEach(this::deleteRecursively);
				}
			}
			Files.deleteIfExists(path);
		} catch (IOException e) {
			logger.warn("Failed to clean up temporary file: {}", path, e);
		}
	}
}
