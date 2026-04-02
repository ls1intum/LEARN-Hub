package com.learnhub.activitymanagement.dto.response;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationsResponse {
	private List<RecommendationItemResponse> activities;
	private Integer total;
	private Map<String, Object> searchCriteria;
	private String generatedAt;
}
