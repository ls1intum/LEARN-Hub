package com.learnhub.usermanagement.dto.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class FavouriteStatusResponseTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void serializesBooleanFlagAsIsFavourited() throws Exception {
		String json = objectMapper.writeValueAsString(new FavouriteStatusResponse(true));

		assertThat(json).contains("\"isFavourited\":true");
		assertThat(json).doesNotContain("\"favourited\":true");
	}
}
