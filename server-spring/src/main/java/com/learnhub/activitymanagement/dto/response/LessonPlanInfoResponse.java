package com.learnhub.activitymanagement.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LessonPlanInfoResponse {
	@JsonProperty("can_generate_lesson_plan")
	private boolean canGenerateLessonPlan;

	@JsonProperty("available_pdfs")
	private int availablePdfs;

	@JsonProperty("missing_pdfs")
	private List<Integer> missingPdfs;
}
