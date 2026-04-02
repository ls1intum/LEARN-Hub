package com.learnhub.documentmanagement.dto.request;

import lombok.Data;

@Data
public class MarkdownPreviewRequest {
	private String markdown;
	private String orientation;
	private String activityName;
}
