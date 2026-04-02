package com.learnhub.usermanagement.dto.response;

import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LessonPlanFavouriteItemResponse {
	private UUID id;
	private String favouriteType;
	private String name;
	private List<UUID> activityIds;
	private LessonPlanDataResponse lessonPlan;
	private String createdAt;
}
