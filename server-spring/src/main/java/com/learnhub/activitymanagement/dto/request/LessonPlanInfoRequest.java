package com.learnhub.activitymanagement.dto.request;

import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class LessonPlanInfoRequest {
	private List<Map<String, Object>> activities;
}
