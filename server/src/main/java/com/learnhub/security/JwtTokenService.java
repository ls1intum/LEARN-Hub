package com.learnhub.security;

import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

	private final JwtEncoder jwtEncoder;
	private final JwtDecoder jwtDecoder;
	private final long expiration;
	private final long refreshExpiration;

	public JwtTokenService(JwtEncoder jwtEncoder, JwtDecoder jwtDecoder, @Value("${jwt.expiration}") long expiration,
			@Value("${jwt.refresh.expiration}") long refreshExpiration) {
		this.jwtEncoder = jwtEncoder;
		this.jwtDecoder = jwtDecoder;
		this.expiration = expiration;
		this.refreshExpiration = refreshExpiration;
	}

	public String generateAccessToken(String username, UUID userId, String role) {
		return generateToken(username, userId, role, "access", expiration);
	}

	public String generateRefreshToken(String username, UUID userId, String role) {
		return generateToken(username, userId, role, "refresh", refreshExpiration);
	}

	public boolean validateRefreshToken(String token) {
		try {
			return "refresh".equals(decode(token).getClaimAsString("type"));
		} catch (JwtException | IllegalArgumentException e) {
			return false;
		}
	}

	public UUID extractUserId(String token) {
		String userId = decode(token).getClaimAsString("userId");
		return userId != null ? UUID.fromString(userId) : null;
	}

	public String extractUsername(String token) {
		return decode(token).getSubject();
	}

	public String extractRole(String token) {
		return decode(token).getClaimAsString("role");
	}

	private String generateToken(String username, UUID userId, String role, String type, long expirationMillis) {
		Instant issuedAt = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder().subject(username).issuedAt(issuedAt)
				.expiresAt(issuedAt.plusMillis(expirationMillis)).claim("userId", userId.toString()).claim("role", role)
				.claim("type", type).build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}

	private Jwt decode(String token) {
		return jwtDecoder.decode(token);
	}
}
