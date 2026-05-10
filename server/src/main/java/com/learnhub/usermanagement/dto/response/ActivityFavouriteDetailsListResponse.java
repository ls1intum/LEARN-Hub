package com.learnhub.usermanagement.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ActivityFavouriteDetailsListResponse {
	private List<ActivityFavouriteDetailResponse> favourites;
	private long total;
	private int limit;
	private int offset;
}
