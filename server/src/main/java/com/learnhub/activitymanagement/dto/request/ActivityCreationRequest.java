package com.learnhub.activitymanagement.dto.request;

import java.util.List;
import lombok.Data;

@Data
public class ActivityCreationRequest {
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
