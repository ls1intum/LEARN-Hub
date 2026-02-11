package com.learnhub.activitymanagement.entity;

import java.util.List;

public class Break {
    private String id;
    private int duration;
    private String description;
    private List<String> reasons;
    
    public Break() {}
    
    public Break(String id, int duration, String description, List<String> reasons) {
        this.id = id;
        this.duration = duration;
        this.description = description;
        this.reasons = reasons;
    }
    
    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public List<String> getReasons() { return reasons; }
    public void setReasons(List<String> reasons) { this.reasons = reasons; }
}
