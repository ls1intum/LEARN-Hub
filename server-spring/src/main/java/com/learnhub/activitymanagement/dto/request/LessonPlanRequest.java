package com.learnhub.activitymanagement.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class LessonPlanRequest {
    private List<Map<String, Object>> activities;
    
    @JsonProperty("search_criteria")
    private Map<String, Object> searchCriteria;
    
    private List<Map<String, Object>> breaks;
    
    @JsonProperty("total_duration")
    private Integer totalDuration;
}
