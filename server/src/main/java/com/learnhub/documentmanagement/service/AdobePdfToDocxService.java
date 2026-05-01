package com.learnhub.documentmanagement.service;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * Converts PDF bytes to DOCX bytes via the Adobe PDF Services REST API
 * (ExportPDF operation). The PDF is uploaded as a cloud asset, an export job
 * is created, polled until completion, and the resulting DOCX is downloaded.
 */
@Service
public class AdobePdfToDocxService {

	private static final Logger logger = LoggerFactory.getLogger(AdobePdfToDocxService.class);

	private static final String IMS_TOKEN_URL = "https://ims-na1.adobelogin.com/ims/token/v3";
	private static final String PDF_SERVICES_BASE_URL = "https://pdf-services.adobe.io";
	private static final int POLL_INTERVAL_MS = 2000;
	private static final int MAX_POLL_ATTEMPTS = 90;

	@Value("${learnhub.adobe.client-id:}")
	private String clientId;

	@Value("${learnhub.adobe.client-secret:}")
	private String clientSecret;

	private final RestTemplate restTemplate = new RestTemplate();
	private final AtomicReference<CachedToken> cachedToken = new AtomicReference<>();

	private record CachedToken(String token, Instant expiresAt) {
		boolean isValid() {
			return Instant.now().isBefore(expiresAt.minusSeconds(60));
		}
	}

	/**
	 * Returns true when both {@code client-id} and {@code client-secret} are
	 * configured. The server starts normally when they are absent, but DOCX export
	 * endpoints will refuse requests until credentials are provided.
	 */
	public boolean isConfigured() {
		return clientId != null && !clientId.isBlank() && clientSecret != null && !clientSecret.isBlank();
	}

	/**
	 * Convert a PDF byte array to a DOCX byte array using Adobe PDF Services.
	 *
	 * @param pdfBytes
	 *            the input PDF
	 * @return the resulting DOCX
	 */
	public byte[] convertPdfToDocx(byte[] pdfBytes) {
		if (!isConfigured()) {
			throw new IllegalStateException(
					"Adobe PDF Services credentials are not configured. Set learnhub.adobe.client-id and learnhub.adobe.client-secret.");
		}
		String accessToken = getAccessToken();
		String assetId = uploadAsset(pdfBytes, accessToken);
		String jobLocation = createExportJob(assetId, accessToken);
		String downloadUri = pollUntilDone(jobLocation, accessToken);
		return downloadResult(downloadUri);
	}

	@SuppressWarnings("unchecked")
	private String getAccessToken() {
		CachedToken cached = cachedToken.get();
		if (cached != null && cached.isValid()) {
			return cached.token();
		}

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
		body.add("grant_type", "client_credentials");
		body.add("client_id", clientId);
		body.add("client_secret", clientSecret);
		body.add("scope", "openid,AdobeID,DCAPI");

		ResponseEntity<Map> response = restTemplate.postForEntity(IMS_TOKEN_URL,
				new HttpEntity<>(body, headers), Map.class);

		Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
		String token = (String) responseBody.get("access_token");
		int expiresIn = ((Number) responseBody.get("expires_in")).intValue();

		cachedToken.set(new CachedToken(token, Instant.now().plusSeconds(expiresIn)));
		logger.debug("Acquired Adobe PDF Services access token (expires in {}s)", expiresIn);
		return token;
	}

	@SuppressWarnings("unchecked")
	private String uploadAsset(byte[] pdfBytes, String accessToken) {
		HttpHeaders headers = buildAuthHeaders(accessToken);
		headers.setContentType(MediaType.APPLICATION_JSON);

		ResponseEntity<Map> createResponse = restTemplate.postForEntity(
				PDF_SERVICES_BASE_URL + "/assets",
				new HttpEntity<>(Map.of("mediaType", "application/pdf"), headers),
				Map.class);

		Map<String, Object> createBody = (Map<String, Object>) createResponse.getBody();
		String uploadUri = (String) createBody.get("uploadUri");
		String assetId = (String) createBody.get("assetID");

		// Use URI (not String) so RestTemplate does not re-encode the presigned query
		// parameters — re-encoding breaks the X-Amz-Credential S3 signature check.
		HttpHeaders uploadHeaders = new HttpHeaders();
		uploadHeaders.setContentType(MediaType.APPLICATION_PDF);
		restTemplate.exchange(URI.create(uploadUri), HttpMethod.PUT, new HttpEntity<>(pdfBytes, uploadHeaders),
				Void.class);

		logger.debug("Uploaded PDF asset: {}", assetId);
		return assetId;
	}

	private String createExportJob(String assetId, String accessToken) {
		HttpHeaders headers = buildAuthHeaders(accessToken);
		headers.setContentType(MediaType.APPLICATION_JSON);

		ResponseEntity<Void> response = restTemplate.postForEntity(
				PDF_SERVICES_BASE_URL + "/operation/exportpdf",
				new HttpEntity<>(Map.of("assetID", assetId, "targetFormat", "docx"), headers),
				Void.class);

		String location = response.getHeaders().getFirst("Location");
		if (location == null) {
			throw new RuntimeException("Adobe PDF Services export job returned no Location header");
		}
		logger.debug("Created export job: {}", location);
		return location;
	}

	@SuppressWarnings("unchecked")
	private String pollUntilDone(String jobLocation, String accessToken) {
		HttpEntity<Void> request = new HttpEntity<>(buildAuthHeaders(accessToken));

		for (int attempt = 0; attempt < MAX_POLL_ATTEMPTS; attempt++) {
			ResponseEntity<Map> response = restTemplate.exchange(URI.create(jobLocation), HttpMethod.GET, request,
					Map.class);
			Map<String, Object> body = (Map<String, Object>) response.getBody();
			String status = (String) body.get("status");

			if ("done".equals(status)) {
				Map<String, Object> asset = (Map<String, Object>) body.get("asset");
				return (String) asset.get("downloadUri");
			}
			if ("failed".equals(status)) {
				throw new RuntimeException("Adobe PDF Services export failed: " + body.get("error"));
			}

			try {
				Thread.sleep(POLL_INTERVAL_MS);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new RuntimeException("Interrupted while polling Adobe PDF Services", e);
			}
		}
		throw new RuntimeException(
				"Adobe PDF Services export timed out after " + MAX_POLL_ATTEMPTS + " poll attempts");
	}

	private byte[] downloadResult(String downloadUri) {
		ResponseEntity<byte[]> response = restTemplate.getForEntity(URI.create(downloadUri), byte[].class);
		byte[] docxBytes = response.getBody();
		logger.info("Downloaded DOCX from Adobe PDF Services ({} bytes)", docxBytes != null ? docxBytes.length : 0);
		return docxBytes;
	}

	private HttpHeaders buildAuthHeaders(String accessToken) {
		HttpHeaders headers = new HttpHeaders();
		headers.set("Authorization", "Bearer " + accessToken);
		headers.set("x-api-key", clientId);
		return headers;
	}
}
