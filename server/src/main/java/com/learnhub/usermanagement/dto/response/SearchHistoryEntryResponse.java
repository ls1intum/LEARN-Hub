package com.learnhub.usermanagement.dto.response;

import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchHistoryEntryResponse {
	private UUID id;
	private Map<String, Object> searchCriteria;
	private String createdAt;
}
