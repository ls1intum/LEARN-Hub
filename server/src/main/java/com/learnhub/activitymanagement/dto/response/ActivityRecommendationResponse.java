package com.learnhub.activitymanagement.dto.response;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ActivityRecommendationResponse extends ActivityResponse {
	private String createdAt;
	private BreakResponse breakAfter;
}
