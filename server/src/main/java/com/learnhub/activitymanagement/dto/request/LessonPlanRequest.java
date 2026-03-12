package com.learnhub.activitymanagement.dto.request;

import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class LessonPlanRequest {
	private List<Map<String, Object>> activities;

	private Map<String, Object> searchCriteria;

	private List<Map<String, Object>> breaks;

	private Integer totalDuration;
}
