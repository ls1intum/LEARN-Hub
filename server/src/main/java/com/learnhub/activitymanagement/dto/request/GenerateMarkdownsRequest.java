package com.learnhub.activitymanagement.dto.request;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Data;

@Data
public class GenerateMarkdownsRequest {
	private UUID documentId;
	private Map<String, Object> metadata;
	private List<String> types;
}
