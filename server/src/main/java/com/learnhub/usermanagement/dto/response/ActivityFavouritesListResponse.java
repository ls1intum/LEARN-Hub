package com.learnhub.usermanagement.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityFavouritesListResponse {
	private List<ActivityFavouriteItemResponse> favourites;
	private PaginationResponse pagination;
}
