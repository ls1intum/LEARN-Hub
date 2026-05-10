package com.learnhub.activitymanagement.repository;

import java.util.List;
import java.util.UUID;

public interface ActivityRepositoryCustom {

	ActivityQueryResult findPublishedActivitiesWithFilters(String name, Integer ageMin, Integer ageMax,
			Integer durationMin, Integer durationMax, List<String> formats, List<String> bloomLevels,
			List<String> mentalLoads, List<String> physicalEnergies, List<String> resourcesNeeded, List<String> topics,
			Integer limit, Integer offset);

	List<UUID> findPublishedActivityIdsByFilters(String name, Integer ageMin, Integer ageMax, Integer durationMin,
			Integer durationMax, List<String> formats, List<String> bloomLevels, List<String> mentalLoads,
			List<String> physicalEnergies, List<String> resourcesNeeded, List<String> topics,
			List<UUID> restrictedActivityIds);
}
