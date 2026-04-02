package com.learnhub.activitymanagement.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivitiesListResponse {
	private long total;
	private List<ActivityResponse> activities;
	private Integer limit;
	private Integer offset;
}
