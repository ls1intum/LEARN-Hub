package com.learnhub.activitymanagement.dto.response;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ActivityResponse {
	private UUID id;
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

	private List<DocumentResponse> documents = new ArrayList<>();

	private List<MarkdownResponse> markdowns = new ArrayList<>();

	private String type = "activity";
}
