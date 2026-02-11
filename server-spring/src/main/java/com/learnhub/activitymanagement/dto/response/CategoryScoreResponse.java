package com.learnhub.activitymanagement.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CategoryScoreResponse {
    
    private String category;
    private int score;
    private int impact;
    
    @JsonProperty("priority_multiplier")
    private double priorityMultiplier;
    
    @JsonProperty("is_priority")
    private boolean isPriority;
    
    public CategoryScoreResponse() {}
    
    public CategoryScoreResponse(String category, int score, int impact, 
                                double priorityMultiplier, boolean isPriority) {
        this.category = category;
        this.score = score;
        this.impact = impact;
        this.priorityMultiplier = priorityMultiplier;
        this.isPriority = isPriority;
    }
    
    // Getters and setters
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    
    public int getImpact() { return impact; }
    public void setImpact(int impact) { this.impact = impact; }
    
    public double getPriorityMultiplier() { return priorityMultiplier; }
    public void setPriorityMultiplier(double priorityMultiplier) { 
        this.priorityMultiplier = priorityMultiplier; 
    }
    
    public boolean isPriority() { return isPriority; }
    public void setPriority(boolean priority) { isPriority = priority; }
}
