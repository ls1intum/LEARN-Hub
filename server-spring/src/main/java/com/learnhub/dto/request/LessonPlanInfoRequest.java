package com.learnhub.dto.request;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class LessonPlanInfoRequest {
    private List<Map<String, Object>> activities;
}
