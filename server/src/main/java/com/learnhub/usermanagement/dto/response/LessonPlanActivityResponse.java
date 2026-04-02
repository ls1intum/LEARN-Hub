package com.learnhub.usermanagement.dto.response;

import com.learnhub.activitymanagement.dto.response.ActivityResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class LessonPlanActivityResponse extends ActivityResponse {
	private String createdAt;
	private LessonPlanBreakResponse breakAfter;
}
