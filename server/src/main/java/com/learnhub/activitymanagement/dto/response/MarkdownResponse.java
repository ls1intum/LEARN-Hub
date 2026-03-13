package com.learnhub.activitymanagement.dto.response;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MarkdownResponse {
	private UUID id;
	private String type;
	private String content;
}
