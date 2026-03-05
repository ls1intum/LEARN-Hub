package com.learnhub.activitymanagement.dto.request;

import java.util.List;
import org.springframework.web.bind.annotation.BindParam;

public record RecommendationRequest(String name, @BindParam("target_age") Integer targetAge, List<String> format,
		@BindParam("bloom_levels") List<String> bloomLevels, @BindParam("target_duration") Integer targetDuration,
		@BindParam("available_resources") List<String> availableResources,
		@BindParam("preferred_topics") List<String> preferredTopics,
		@BindParam("priority_categories") List<String> priorityCategories,
		@BindParam("include_breaks") Boolean includeBreaks, @BindParam("max_activity_count") Integer maxActivityCount,
		Integer limit) {

	public RecommendationRequest {
		if (includeBreaks == null)
			includeBreaks = false;
		if (maxActivityCount == null)
			maxActivityCount = 2;
		if (limit == null)
			limit = 10;
	}
}
