package com.learnhub.activitymanagement.dto.response;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationItemResponse {
	private List<ActivityRecommendationResponse> activities;
	private Integer score;
	private Map<String, CategoryScoreResponse> scoreBreakdown;
}
