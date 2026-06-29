package com.learnhub.activitymanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GenerateMarkdownsResponse {
	private String documentId;
	private String coverSheetMarkdown;
	private String lessonPlanMarkdown;
	private String backgroundKnowledgeMarkdown;
	private String boardImageMarkdown;
	private String exerciseMarkdown;
	private String exerciseSolutionMarkdown;
}
