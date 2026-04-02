package com.learnhub.usermanagement.dto.response;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityFavouriteItemResponse {
	private UUID id;
	private String favouriteType;
	private UUID activityId;
	private String name;
	private String createdAt;
}
