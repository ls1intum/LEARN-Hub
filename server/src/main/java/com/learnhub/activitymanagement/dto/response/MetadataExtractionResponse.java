package com.learnhub.activitymanagement.dto.response;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MetadataExtractionResponse {
	private String documentId;
	private Map<String, Object> extractedData;
	private double extractionConfidence;
	private String extractionQuality;
}
