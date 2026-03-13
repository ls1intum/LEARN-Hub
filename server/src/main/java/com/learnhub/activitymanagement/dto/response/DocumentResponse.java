package com.learnhub.activitymanagement.dto.response;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DocumentResponse {
	private UUID id;
	private String filename;
	private Long fileSize;
	private String type;
}
