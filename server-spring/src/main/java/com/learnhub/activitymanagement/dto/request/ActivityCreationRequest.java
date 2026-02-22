package com.learnhub.activitymanagement.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class ActivityCreationRequest {
    private String name;
    private String description;
    private String source;
    
    @JsonProperty("age_min")
    private Integer ageMin;
    
    @JsonProperty("age_max")
    private Integer ageMax;
    
    private String format;
    
    @JsonProperty("bloom_level")
    private String bloomLevel;
    
    @JsonProperty("duration_min_minutes")
    private Integer durationMinMinutes;
    
    @JsonProperty("duration_max_minutes")
    private Integer durationMaxMinutes;
    
    @JsonProperty("mental_load")
    private String mentalLoad;
    
    @JsonProperty("physical_energy")
    private String physicalEnergy;
    
    @JsonProperty("prep_time_minutes")
    private Integer prepTimeMinutes;
    
    @JsonProperty("cleanup_time_minutes")
    private Integer cleanupTimeMinutes;
    
    @JsonProperty("resources_needed")
    private List<String> resourcesNeeded;
    
    private List<String> topics;
    
    @JsonProperty("document_id")
    private Long documentId;
}
