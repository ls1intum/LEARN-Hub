package com.learnhub.documentmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response for the dev-only test-markdown endpoint. Only the fields relevant to
 * the requested type are populated; all others are null.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestMarkdownResponse {
	private String exerciseMarkdown;
	private String exerciseSolutionMarkdown;
	private String coverSheetMarkdown;
	private String lessonPlanMarkdown;
	private String backgroundKnowledgeMarkdown;
	private String boardImageMarkdown;
}
