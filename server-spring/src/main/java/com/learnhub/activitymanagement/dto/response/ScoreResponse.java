package com.learnhub.activitymanagement.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class ScoreResponse {
    
    @JsonProperty("total_score")
    private int totalScore;
    
    @JsonProperty("category_scores")
    private Map<String, CategoryScoreResponse> categoryScores;
    
    @JsonProperty("is_sequence")
    private boolean isSequence;
    
    @JsonProperty("activity_count")
    private int activityCount;
    
    public ScoreResponse() {}
    
    public ScoreResponse(int totalScore, Map<String, CategoryScoreResponse> categoryScores, 
                        boolean isSequence, int activityCount) {
        this.totalScore = totalScore;
        this.categoryScores = categoryScores;
        this.isSequence = isSequence;
        this.activityCount = activityCount;
    }
    
    // Getters and setters
    public int getTotalScore() { return totalScore; }
    public void setTotalScore(int totalScore) { this.totalScore = totalScore; }
    
    public Map<String, CategoryScoreResponse> getCategoryScores() { return categoryScores; }
    public void setCategoryScores(Map<String, CategoryScoreResponse> categoryScores) { 
        this.categoryScores = categoryScores; 
    }
    
    public boolean isSequence() { return isSequence; }
    public void setSequence(boolean sequence) { isSequence = sequence; }
    
    public int getActivityCount() { return activityCount; }
    public void setActivityCount(int activityCount) { this.activityCount = activityCount; }
}
