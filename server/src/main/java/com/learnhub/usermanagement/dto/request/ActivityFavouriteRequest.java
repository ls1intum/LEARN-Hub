package com.learnhub.usermanagement.dto.request;

import java.util.UUID;
import lombok.Data;

@Data
public class ActivityFavouriteRequest {
	private UUID activityId;
	private String name;
}
