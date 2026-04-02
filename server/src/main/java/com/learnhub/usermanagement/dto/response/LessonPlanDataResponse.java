package com.learnhub.usermanagement.dto.response;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LessonPlanDataResponse {
	private List<LessonPlanActivityResponse> activities;
	private Integer totalDurationMinutes;
	private List<LessonPlanBreakResponse> breaks;
	private String ordering_strategy;
	private String createdAt;
	private String title;
	private Map<String, Object> searchCriteria;
}
