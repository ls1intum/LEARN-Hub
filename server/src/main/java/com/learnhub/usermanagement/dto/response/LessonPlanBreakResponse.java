package com.learnhub.usermanagement.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LessonPlanBreakResponse {
	private String id;
	private Integer duration;
	private String description;
	private List<String> reasons;
}
