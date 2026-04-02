package com.learnhub.activitymanagement.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BreakResponse {
	private String id;
	private Integer duration;
	private String description;
	private List<String> reasons;
}
