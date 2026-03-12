package com.learnhub.activitymanagement.dto.request;

import java.util.List;

public record ActivityFilterRequest(String name, Integer ageMin,
		Integer ageMax, Integer durationMin,
		Integer durationMax, List<String> format,
		List<String> bloomLevel, String mentalLoad,
		String physicalEnergy,
		List<String> resourcesNeeded, List<String> topics, Integer limit,
		Integer offset) {

	public ActivityFilterRequest {
		if (limit == null)
			limit = 10;
		if (offset == null)
			offset = 0;
	}
}
