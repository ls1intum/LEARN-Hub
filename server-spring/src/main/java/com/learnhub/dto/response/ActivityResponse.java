package com.learnhub.dto.response;

import com.learnhub.model.enums.ActivityFormat;
import com.learnhub.model.enums.BloomLevel;
import com.learnhub.model.enums.EnergyLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ActivityResponse {
    private Long id;
    private String name;
    private String description;
    private String source;
    private Integer ageMin;
    private Integer ageMax;
    private String format;
    private String bloomLevel;
    private Integer durationMinMinutes;
    private Integer durationMaxMinutes;
    private String mentalLoad;
    private String physicalEnergy;
    private Integer prepTimeMinutes;
    private Integer cleanupTimeMinutes;
    private List<String> resourcesNeeded;
    private List<String> topics;
    private Long documentId;
}
