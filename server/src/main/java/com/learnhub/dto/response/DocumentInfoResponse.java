package com.learnhub.dto.response;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentInfoResponse {
	private UUID id;
	private String filename;
	private Long fileSize;
	private String confidenceScore;
	private String extractionQuality;
	private String createdAt;
}
