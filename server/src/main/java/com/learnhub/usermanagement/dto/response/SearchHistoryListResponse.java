package com.learnhub.usermanagement.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchHistoryListResponse {
	private List<SearchHistoryEntryResponse> searchHistory;
	private PaginationResponse pagination;
}
