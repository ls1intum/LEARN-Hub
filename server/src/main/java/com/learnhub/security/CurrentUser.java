package com.learnhub.security;

import java.util.UUID;
import org.springframework.security.core.Authentication;

public final class CurrentUser {

	private CurrentUser() {
	}

	public static UUID getUserId(Authentication authentication) {
		if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser authenticatedUser)) {
			return null;
		}
		return authenticatedUser.getUserId();
	}
}
