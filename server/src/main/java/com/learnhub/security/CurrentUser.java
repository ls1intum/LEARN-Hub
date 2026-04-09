package com.learnhub.security;

import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.util.StringUtils;

public final class CurrentUser {

	private CurrentUser() {
	}

	public static UUID getUserId(Authentication authentication) {
		if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
			return null;
		}

		String userId = jwtAuthentication.getToken().getClaimAsString("userId");
		if (!StringUtils.hasText(userId)) {
			return null;
		}

		try {
			return UUID.fromString(userId);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
}
