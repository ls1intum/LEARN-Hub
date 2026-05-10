package com.learnhub.security;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

public final class AuthenticatedUser implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	private final UUID userId;
	private final String email;
	private final String role;

	public AuthenticatedUser(UUID userId, String email, String role) {
		this.userId = userId;
		this.email = email;
		this.role = role;
	}

	public UUID getUserId() {
		return userId;
	}

	public String getEmail() {
		return email;
	}

	public String getRole() {
		return role;
	}

	@Override
	public String toString() {
		return email;
	}
}
