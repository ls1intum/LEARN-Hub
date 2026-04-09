package com.learnhub.usermanagement.controller;

import com.learnhub.dto.request.EmailRequest;
import com.learnhub.dto.response.ErrorResponse;
import com.learnhub.dto.response.MessageResponse;
import com.learnhub.security.CurrentUser;
import com.learnhub.usermanagement.dto.request.CreateUserRequest;
import com.learnhub.usermanagement.dto.request.LoginRequest;
import com.learnhub.usermanagement.dto.request.PasswordResetRequest;
import com.learnhub.usermanagement.dto.request.RefreshTokenRequest;
import com.learnhub.usermanagement.dto.request.TeacherRegistrationRequest;
import com.learnhub.usermanagement.dto.request.UpdateProfileRequest;
import com.learnhub.usermanagement.dto.request.UpdateUserRequest;
import com.learnhub.usermanagement.dto.request.VerifyCodeRequest;
import com.learnhub.usermanagement.dto.response.LoginResponse;
import com.learnhub.usermanagement.dto.response.RefreshTokenResponse;
import com.learnhub.usermanagement.dto.response.UserEnvelopeResponse;
import com.learnhub.usermanagement.dto.response.UserMessageResponse;
import com.learnhub.usermanagement.dto.response.UserResponse;
import com.learnhub.usermanagement.dto.response.UsersListResponse;
import com.learnhub.usermanagement.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Authentication and user management endpoints")
public class AuthController {

	private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

	@Autowired
	private AuthService authService;

	@PostMapping("/register-teacher")
	@PreAuthorize("permitAll()")
	@Operation(summary = "Register a new teacher", description = "Register a new teacher account and send verification code")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Teacher registered", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserMessageResponse.class)))})
	public ResponseEntity<?> registerTeacher(@Valid @RequestBody TeacherRegistrationRequest request) {
		logger.info("POST /api/auth/register-teacher - Register teacher called with email={}", request.getEmail());
		try {
			UserResponse user = authService.registerTeacher(request);
			logger.info("POST /api/auth/register-teacher - Teacher registered successfully with id={}", user.getId());
			return ResponseEntity.status(201).body(new UserMessageResponse(
					"Teacher registered successfully. Credentials have been sent via email.", user));
		} catch (Exception e) {
			logger.error("POST /api/auth/register-teacher - Registration failed: {}", e.getMessage());
			return ResponseEntity.badRequest().body(ErrorResponse.of(e.getMessage()));
		}
	}

	@PostMapping("/verification-code")
	@PreAuthorize("permitAll()")
	@Operation(summary = "Request verification code", description = "Send a verification code to the user's email address")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Verification code sent", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageResponse.class)))})
	public ResponseEntity<?> requestVerificationCode(@RequestBody EmailRequest request) {
		String email = request.getEmail();
		logger.info("POST /api/auth/verification-code - Verification code requested for email={}", email);
		try {
			authService.requestVerificationCode(email);
			return ResponseEntity.ok(MessageResponse.of("Verification code sent"));
		} catch (Exception e) {
			logger.error("POST /api/auth/verification-code - Failed to send verification code: {}", e.getMessage());
			return ResponseEntity.badRequest().body(ErrorResponse.of(e.getMessage()));
		}
	}

	@PostMapping("/verify")
	@PreAuthorize("permitAll()")
	@Operation(summary = "Verify code and login", description = "Verify the code and complete login process")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Login result", content = @Content(mediaType = "application/json", schema = @Schema(implementation = LoginResponse.class)))})
	public ResponseEntity<?> verifyCode(@Valid @RequestBody VerifyCodeRequest request) {
		logger.info("POST /api/auth/verify - Verify code called for email={}", request.getEmail());
		try {
			LoginResponse response = authService.verifyCode(request);
			logger.info("POST /api/auth/verify - Verification successful for email={}", request.getEmail());
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			logger.error("POST /api/auth/verify - Verification failed for email={}: {}", request.getEmail(),
					e.getMessage());
			return ResponseEntity.badRequest().body(ErrorResponse.of(e.getMessage()));
		}
	}

	@PostMapping("/login")
	@PreAuthorize("permitAll()")
	@Operation(summary = "Login with password", description = "Login with email and password (admin or teacher)")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Login result", content = @Content(mediaType = "application/json", schema = @Schema(implementation = LoginResponse.class)))})
	public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
		logger.info("POST /api/auth/login - Login called for email={}", request.getEmail());
		try {
			LoginResponse response = authService.login(request);
			logger.info("POST /api/auth/login - Login successful for email={}", request.getEmail());
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			logger.error("POST /api/auth/login - Login failed for email={}: {}", request.getEmail(), e.getMessage());
			return ResponseEntity.badRequest().body(ErrorResponse.of(e.getMessage()));
		}
	}

	@GetMapping("/me")
	@PreAuthorize("isAuthenticated()")
	@SecurityRequirement(name = "BearerAuth")
	@Operation(summary = "Get current user", description = "Get information about the currently authenticated user")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Current user", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponse.class)))})
	public ResponseEntity<?> getCurrentUser(Authentication authentication) {
		logger.info("GET /api/auth/me - Get current user called");
		try {
			UUID userId = CurrentUser.getUserId(authentication);
			if (userId == null) {
				logger.error("GET /api/auth/me - Unauthorized: userId not found in request");
				return ResponseEntity.status(401).body(ErrorResponse.of("Unauthorized"));
			}
			UserResponse user = authService.getUserById(userId);
			if (user == null) {
				logger.error("GET /api/auth/me - User not found for id={}", userId);
				return ResponseEntity.status(404).body(ErrorResponse.of("User not found"));
			}
			return ResponseEntity.ok(user);
		} catch (Exception e) {
			logger.error("GET /api/auth/me - Failed to retrieve current user: {}", e.getMessage());
			return ResponseEntity.status(500).body(ErrorResponse.of(e.getMessage()));
		}
	}

	@PostMapping("/refresh")
	@PreAuthorize("permitAll()")
	@Operation(summary = "Refresh token", description = "Refresh the JWT access token using refresh token")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Refreshed tokens", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RefreshTokenResponse.class)))})
	public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenRequest request) {
		logger.info("POST /api/auth/refresh - Token refresh called");
		try {
			LoginResponse response = authService.refreshToken(request.getRefreshToken());
			return ResponseEntity.ok(new RefreshTokenResponse(response.getAccessToken(), response.getRefreshToken()));
		} catch (Exception e) {
			logger.error("POST /api/auth/refresh - Token refresh failed: {}", e.getMessage());
			return ResponseEntity.status(401).body(ErrorResponse.of(e.getMessage()));
		}
	}

	@PostMapping("/logout")
	@PreAuthorize("isAuthenticated()")
	@SecurityRequirement(name = "BearerAuth")
	@Operation(summary = "Logout", description = "Logout current user")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Logout confirmation", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageResponse.class)))})
	public ResponseEntity<?> logout() {
		logger.info("POST /api/auth/logout - Logout called");
		try {
			return ResponseEntity.ok(MessageResponse.of("Logged out successfully"));
		} catch (Exception e) {
			logger.error("POST /api/auth/logout - Logout failed: {}", e.getMessage());
			return ResponseEntity.badRequest().body(ErrorResponse.of(e.getMessage()));
		}
	}

	@PostMapping("/reset-password")
	@PreAuthorize("permitAll()")
	@Operation(summary = "Reset password", description = "Reset password for a teacher")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Password reset confirmation", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageResponse.class)))})
	public ResponseEntity<?> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
		logger.info("POST /api/auth/reset-password - Password reset called for email={}", request.getEmail());
		try {
			authService.resetPassword(request.getEmail());
			logger.info("POST /api/auth/reset-password - Password reset successfully for email={}", request.getEmail());
			return ResponseEntity
					.ok(MessageResponse.of("Password reset successfully. New credentials have been sent via email."));
		} catch (RuntimeException e) {
			if (e.getMessage().equals("Teacher not found")) {
				logger.error("POST /api/auth/reset-password - Teacher not found for email={}", request.getEmail());
				return ResponseEntity.status(404).body(ErrorResponse.of("Teacher not found"));
			}
			logger.error("POST /api/auth/reset-password - Password reset failed: {}", e.getMessage());
			return ResponseEntity.badRequest().body(ErrorResponse.of(e.getMessage()));
		} catch (Exception e) {
			logger.error("POST /api/auth/reset-password - Unexpected error during password reset: {}", e.getMessage());
			return ResponseEntity.status(500).body(ErrorResponse.of("Failed to reset password: " + e.getMessage()));
		}
	}

	// User management endpoints (admin only)
	@GetMapping("/users")
	@PreAuthorize("hasRole('ADMIN')")
	@SecurityRequirement(name = "BearerAuth")
	@Operation(summary = "Get all users", description = "Get list of all users (admin only)")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Users list", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UsersListResponse.class)))})
	public ResponseEntity<?> getUsers() {
		logger.info("GET /api/auth/users - Get all users called");
		try {
			List<UserResponse> users = authService.getAllUsers();
			return ResponseEntity.ok(new UsersListResponse(users));
		} catch (Exception e) {
			logger.error("GET /api/auth/users - Failed to retrieve users: {}", e.getMessage());
			return ResponseEntity.status(500).body(ErrorResponse.of(e.getMessage()));
		}
	}

	@PostMapping("/users")
	@PreAuthorize("hasRole('ADMIN')")
	@SecurityRequirement(name = "BearerAuth")
	@Operation(summary = "Create user", description = "Create a new user (admin only)")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Created user", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserEnvelopeResponse.class)))})
	public ResponseEntity<?> createUser(@Valid @RequestBody CreateUserRequest request) {
		logger.info("POST /api/auth/users - Create user called with email={}, role={}", request.getEmail(),
				request.getRole());
		try {
			UserResponse user = authService.createUser(request.getEmail(), request.getFirstName(),
					request.getLastName(), request.getRole(), request.getPassword());
			logger.info("POST /api/auth/users - User created with id={}", user.getId());
			return ResponseEntity.status(201).body(new UserEnvelopeResponse(user));
		} catch (RuntimeException e) {
			if (e.getMessage().contains("already exists")) {
				logger.error("POST /api/auth/users - User already exists: {}", e.getMessage());
				return ResponseEntity.status(409).body(ErrorResponse.of(e.getMessage()));
			}
			logger.error("POST /api/auth/users - Failed to create user: {}", e.getMessage());
			return ResponseEntity.badRequest().body(ErrorResponse.of(e.getMessage()));
		} catch (Exception e) {
			logger.error("POST /api/auth/users - Unexpected error creating user: {}", e.getMessage());
			return ResponseEntity.status(500).body(ErrorResponse.of(e.getMessage()));
		}
	}

	@PutMapping("/users/{userId}")
	@PreAuthorize("hasRole('ADMIN')")
	@SecurityRequirement(name = "BearerAuth")
	@Operation(summary = "Update user", description = "Update user details (admin only)")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Updated user", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserEnvelopeResponse.class)))})
	public ResponseEntity<?> updateUser(@PathVariable UUID userId, @Valid @RequestBody UpdateUserRequest request) {
		logger.info("PUT /api/auth/users/{} - Update user called", userId);
		try {
			UserResponse user = authService.updateUser(userId, request.getEmail(), request.getFirstName(),
					request.getLastName(), request.getRole(), request.getPassword());
			logger.info("PUT /api/auth/users/{} - User updated successfully", userId);
			return ResponseEntity.ok(new UserEnvelopeResponse(user));
		} catch (RuntimeException e) {
			if (e.getMessage().contains("not found")) {
				logger.error("PUT /api/auth/users/{} - User not found: {}", userId, e.getMessage());
				return ResponseEntity.status(404).body(ErrorResponse.of(e.getMessage()));
			}
			if (e.getMessage().contains("already exists")) {
				logger.error("PUT /api/auth/users/{} - Email conflict: {}", userId, e.getMessage());
				return ResponseEntity.status(409).body(ErrorResponse.of(e.getMessage()));
			}
			logger.error("PUT /api/auth/users/{} - Failed to update user: {}", userId, e.getMessage());
			return ResponseEntity.badRequest().body(ErrorResponse.of(e.getMessage()));
		} catch (Exception e) {
			logger.error("PUT /api/auth/users/{} - Unexpected error updating user: {}", userId, e.getMessage());
			return ResponseEntity.status(500).body(ErrorResponse.of(e.getMessage()));
		}
	}

	@DeleteMapping("/users/{userId}")
	@PreAuthorize("hasRole('ADMIN')")
	@SecurityRequirement(name = "BearerAuth")
	@Operation(summary = "Delete user", description = "Delete a user (admin only)")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Delete confirmation", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageResponse.class)))})
	public ResponseEntity<?> deleteUser(@PathVariable UUID userId, Authentication authentication) {
		logger.info("DELETE /api/auth/users/{} - Delete user called", userId);
		try {
			UUID currentUserId = CurrentUser.getUserId(authentication);
			if (currentUserId == null) {
				logger.error("DELETE /api/auth/users/{} - Unauthorized: userId not found in token", userId);
				return ResponseEntity.status(401).body(ErrorResponse.of("Unauthorized"));
			}
			boolean deleted = authService.deleteUser(userId, currentUserId);
			if (!deleted) {
				logger.error("DELETE /api/auth/users/{} - User not found", userId);
				return ResponseEntity.status(404).body(ErrorResponse.of("User not found"));
			}
			logger.info("DELETE /api/auth/users/{} - User deleted successfully", userId);
			return ResponseEntity.ok(MessageResponse.of("User deleted successfully"));
		} catch (RuntimeException e) {
			logger.error("DELETE /api/auth/users/{} - Failed to delete user: {}", userId, e.getMessage());
			return ResponseEntity.badRequest().body(ErrorResponse.of(e.getMessage()));
		} catch (Exception e) {
			logger.error("DELETE /api/auth/users/{} - Unexpected error deleting user: {}", userId, e.getMessage());
			return ResponseEntity.status(500).body(ErrorResponse.of(e.getMessage()));
		}
	}

	@PutMapping("/me")
	@PreAuthorize("isAuthenticated()")
	@SecurityRequirement(name = "BearerAuth")
	@Operation(summary = "Update profile", description = "Update current user's profile")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Updated profile", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponse.class)))})
	public ResponseEntity<?> updateProfile(@Valid @RequestBody UpdateProfileRequest request, Authentication authentication) {
		logger.info("PUT /api/auth/me - Update profile called");
		try {
			UUID userId = CurrentUser.getUserId(authentication);
			if (userId == null) {
				logger.error("PUT /api/auth/me - Unauthorized: userId not found in request");
				return ResponseEntity.status(401).body(ErrorResponse.of("Unauthorized"));
			}
			UserResponse user = authService.updateProfile(userId, request.getEmail(), request.getFirstName(),
					request.getLastName(), request.getPassword());
			logger.info("PUT /api/auth/me - Profile updated successfully for userId={}", userId);
			return ResponseEntity.ok(user);
		} catch (RuntimeException e) {
			if (e.getMessage().contains("not found")) {
				logger.error("PUT /api/auth/me - User not found: {}", e.getMessage());
				return ResponseEntity.status(404).body(ErrorResponse.of(e.getMessage()));
			}
			if (e.getMessage().contains("already exists")) {
				logger.error("PUT /api/auth/me - Email conflict: {}", e.getMessage());
				return ResponseEntity.status(409).body(ErrorResponse.of(e.getMessage()));
			}
			logger.error("PUT /api/auth/me - Failed to update profile: {}", e.getMessage());
			return ResponseEntity.badRequest().body(ErrorResponse.of(e.getMessage()));
		} catch (Exception e) {
			logger.error("PUT /api/auth/me - Unexpected error updating profile: {}", e.getMessage());
			return ResponseEntity.status(500).body(ErrorResponse.of(e.getMessage()));
		}
	}

	@DeleteMapping("/me")
	@PreAuthorize("isAuthenticated()")
	@SecurityRequirement(name = "BearerAuth")
	@Operation(summary = "Delete account", description = "Delete current user's account")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Delete confirmation", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageResponse.class)))})
	public ResponseEntity<?> deleteAccount(Authentication authentication) {
		logger.info("DELETE /api/auth/me - Delete account called");
		try {
			UUID userId = CurrentUser.getUserId(authentication);
			if (userId == null) {
				logger.error("DELETE /api/auth/me - Unauthorized: userId not found in request");
				return ResponseEntity.status(401).body(ErrorResponse.of("Unauthorized"));
			}
			boolean deleted = authService.deleteAccount(userId);
			if (!deleted) {
				logger.error("DELETE /api/auth/me - User not found for id={}", userId);
				return ResponseEntity.status(404).body(ErrorResponse.of("User not found"));
			}
			logger.info("DELETE /api/auth/me - Account deleted successfully for userId={}", userId);
			return ResponseEntity.ok(MessageResponse.of("Account deleted successfully"));
		} catch (Exception e) {
			logger.error("DELETE /api/auth/me - Unexpected error deleting account: {}", e.getMessage());
			return ResponseEntity.status(500).body(ErrorResponse.of(e.getMessage()));
		}
	}
}
