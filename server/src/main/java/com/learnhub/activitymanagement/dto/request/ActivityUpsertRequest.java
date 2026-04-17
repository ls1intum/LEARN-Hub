package com.learnhub.activitymanagement.dto.request;

import java.util.List;
import java.util.UUID;
import lombok.Data;

@Data
public class ActivityUpsertRequest {
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
	private UUID documentId;
	private String artikulationsschemaMarkdown;
	private String deckblattMarkdown;
	private String hintergrundwissenMarkdown;
	private String uebungMarkdown;
	private String uebungLoesungMarkdown;
}
