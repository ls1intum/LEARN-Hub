package com.learnhub.activitymanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GenerateMarkdownsResponse {
	private String documentId;
	private String deckblattMarkdown;
	private String artikulationsschemaMarkdown;
	private String hintergrundwissenMarkdown;
}
