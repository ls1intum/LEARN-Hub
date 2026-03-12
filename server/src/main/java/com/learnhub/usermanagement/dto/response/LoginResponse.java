package com.learnhub.usermanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {
	private String accessToken;

	private String refreshToken;

	private String tokenType = "Bearer";

	private UserResponse user;

	public LoginResponse(String accessToken, String refreshToken, UserResponse user) {
		this.accessToken = accessToken;
		this.refreshToken = refreshToken;
		this.user = user;
	}
}
