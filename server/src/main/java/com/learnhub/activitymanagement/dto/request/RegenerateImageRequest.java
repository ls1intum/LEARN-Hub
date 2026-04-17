package com.learnhub.activitymanagement.dto.request;

import lombok.Data;

@Data
public class RegenerateImageRequest {
	private String imageId;
	private String description;
	private String customPrompt;
	private String exerciseContext;
}
