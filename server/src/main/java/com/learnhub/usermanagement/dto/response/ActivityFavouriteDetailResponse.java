package com.learnhub.usermanagement.dto.response;

import com.learnhub.activitymanagement.dto.response.ActivityResponse;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ActivityFavouriteDetailResponse {
	private UUID favouriteId;
	private String favouriteCreatedAt;
	private ActivityResponse activity;
}
