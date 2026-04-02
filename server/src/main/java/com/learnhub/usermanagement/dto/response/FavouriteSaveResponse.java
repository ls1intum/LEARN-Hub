package com.learnhub.usermanagement.dto.response;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FavouriteSaveResponse {
	private String message;
	private UUID favouriteId;
}
