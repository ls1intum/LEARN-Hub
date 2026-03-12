package com.learnhub.activitymanagement.dto.request;

import java.util.List;

public record RecommendationRequest(String name, Integer targetAge, List<String> format, List<String> bloomLevels,
		Integer targetDuration, List<String> availableResources, List<String> preferredTopics,
		List<String> priorityCategories, Boolean includeBreaks, Integer maxActivityCount, Integer limit) {

	public RecommendationRequest {
		if (includeBreaks == null)
			includeBreaks = false;
		if (maxActivityCount == null)
			maxActivityCount = 2;
		if (limit == null)
			limit = 10;
	}
}
