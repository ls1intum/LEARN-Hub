package com.learnhub.activitymanagement.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LessonPlanInfoResponse {
	private boolean canGenerateLessonPlan;

	private int availablePdfs;

	private List<Integer> missingPdfs;
}
