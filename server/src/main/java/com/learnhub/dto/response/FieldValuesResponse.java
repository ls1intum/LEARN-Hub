package com.learnhub.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FieldValuesResponse {
	private List<String> format;

	@JsonProperty("resources_available")
	private List<String> resourcesAvailable;

	private List<String> bloomLevel;
	private List<String> topics;
	private List<String> mentalLoad;
	private List<String> physicalEnergy;
	private List<String> priorityCategories;
}
