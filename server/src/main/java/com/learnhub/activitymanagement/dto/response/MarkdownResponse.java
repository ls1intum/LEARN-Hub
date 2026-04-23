package com.learnhub.activitymanagement.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MarkdownResponse {
	private UUID id;
	private String type;
	private String content;
	private boolean landscape;
}
