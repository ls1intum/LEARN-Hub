package com.learnhub.usermanagement.dto.request;

import com.learnhub.usermanagement.dto.response.LessonPlanDataResponse;
import java.util.List;
import java.util.UUID;
import lombok.Data;

@Data
public class LessonPlanFavouriteRequest {
	private List<UUID> activityIds;
	private String name;
	private LessonPlanDataResponse lessonPlan;
}
