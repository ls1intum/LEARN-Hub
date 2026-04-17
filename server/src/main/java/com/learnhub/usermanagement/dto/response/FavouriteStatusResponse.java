package com.learnhub.usermanagement.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FavouriteStatusResponse {
	@JsonProperty("isFavourited")
	private boolean favourited;
}
