package com.learnhub.usermanagement.controller;

import com.learnhub.dto.response.ErrorResponse;
import com.learnhub.dto.response.MessageResponse;
import com.learnhub.usermanagement.dto.request.CreateUserRequest;
import com.learnhub.usermanagement.dto.request.LoginRequest;
import com.learnhub.usermanagement.dto.request.PasswordResetRequest;
import com.learnhub.usermanagement.dto.request.RefreshTokenRequest;
import com.learnhub.usermanagement.dto.request.TeacherRegistrationRequest;
import com.learnhub.usermanagement.dto.request.UpdateProfileRequest;
import com.learnhub.usermanagement.dto.request.UpdateUserRequest;
import com.learnhub.usermanagement.dto.request.VerifyCodeRequest;
import com.learnhub.usermanagement.dto.response.LoginResponse;
import com.learnhub.usermanagement.dto.response.UserResponse;
import com.learnhub.usermanagement.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
	public ResponseEntity<?> registerTeacher(@Valid @RequestBody TeacherRegistrationRequest request) {
		logger.info("POST /api/auth/register-teacher - Register teacher called with email={}", request.getEmail());
		try {
			UserResponse user = authService.registerTeacher(request);
			logger.info("POST /api/auth/register-teacher - Teacher registered successfully with id={}", user.getId());
			return ResponseEntity.ok(user);
		} catch (Exception e) {
			logger.error("POST /api/auth/register-teacher - Registration failed: {}", e.getMessage());
			return ResponseEntity.badRequest().body(ErrorResponse.of(e.getMessage()));
		}
	}

	@PostMapping("/verification-code")
	@PreAuthorize("permitAll()")
	@Operation(summary = "Request verification code", description = "Send a verification code to the user's email address")
	public ResponseEntity<?> requestVerificationCode(@RequestBody Map<String, String> request) {
		String email = request.get("email");
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
	public ResponseEntity<?> verifyCode(@Valid @RequestBody VerifyCodeRequest request) {
		logger.info("POST /api/auth/verify - Verify code called for email={}", request.getEmail());
		try {
			LoginResponse response = authService.verifyCode(request);
			logger.info("POST /api/auth/verify - Verification successful for email={}", request.getEmail());
			Map<String, Object> result = new HashMap<>();
			result.put("user", response.getUser());
			result.put("access_token", response.getAccessToken());
			result.put("refresh_token", response.getRefreshToken());
			result.put("token_type", "Bearer");
			return ResponseEntity.ok(result);
		} catch (Exception e) {
			logger.error("POST /api/auth/verify - Verification failed for email={}: {}", request.getEmail(),
					e.getMessage());
			return ResponseEntity.badRequest().body(ErrorResponse.of(e.getMessage()));
		}
	}

	@PostMapping("/login")
	@PreAuthorize("permitAll()")
	@Operation(summary = "Login with password", description = "Login with email and password (admin or teacher)")
	public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
		logger.info("POST /api/auth/login - Login called for email={}", request.getEmail());
		try {
			LoginResponse response = authService.login(request);
			logger.info("POST /api/auth/login - Login successful for email={}", request.getEmail());
			Map<String, Object> result = new HashMap<>();
			result.put("user", response.getUser());
			result.put("access_token", response.getAccessToken());
			result.put("refresh_token", response.getRefreshToken());
			result.put("token_type", "Bearer");
			return ResponseEntity.ok(result);
		} catch (Exception e) {
			logger.error("POST /api/auth/login - Login failed for email={}: {}", request.getEmail(), e.getMessage());
			return ResponseEntity.badRequest().body(ErrorResponse.of(e.getMessage()));
		}
	}

	@PostMapping("/admin/login")
	@PreAuthorize("permitAll()")
	@Operation(summary = "Admin login", description = "Login with admin credentials")
	public ResponseEntity<?> adminLogin(@Valid @RequestBody LoginRequest request) {
		logger.info("POST /api/auth/admin/login - Admin login called for email={}", request.getEmail());
		try {
			LoginResponse response = authService.login(request);
			// Verify user is admin
			if (!"ADMIN".equals(response.getUser().getRole())) {
				logger.error("POST /api/auth/admin/login - Non-admin login attempt for email={}", request.getEmail());
				return ResponseEntity.status(401).body(ErrorResponse.of("Unauthorized"));
			}
			logger.info("POST /api/auth/admin/login - Admin login successful for email={}", request.getEmail());
			Map<String, Object> result = new HashMap<>();
			result.put("user", response.getUser());
			result.put("access_token", response.getAccessToken());
			result.put("refresh_token", response.getRefreshToken());
			result.put("token_type", "Bearer");
			return ResponseEntity.ok(result);
		} catch (Exception e) {
			logger.error("POST /api/auth/admin/login - Admin login failed: {}", e.getMessage());
			return ResponseEntity.badRequest().body(ErrorResponse.of(e.getMessage()));
		}
	}

	@GetMapping("/me")
	@PreAuthorize("isAuthenticated()")
	@SecurityRequirement(name = "BearerAuth")
	@Operation(summary = "Get current user", description = "Get information about the currently authenticated user")
	public ResponseEntity<?> getCurrentUser(HttpServletRequest request) {
		logger.info("GET /api/auth/me - Get current user called");
		try {
			UUID userId = (UUID) request.getAttribute("userId");
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
	public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenRequest request) {
		logger.info("POST /api/auth/refresh - Token refresh called");
		try {
			LoginResponse response = authService.refreshToken(request.getRefreshToken());
			Map<String, String> result = new HashMap<>();
			result.put("access_token", response.getAccessToken());
			result.put("refresh_token", response.getRefreshToken());
			return ResponseEntity.ok(result);
		} catch (Exception e) {
			logger.error("POST /api/auth/refresh - Token refresh failed: {}", e.getMessage());
			return ResponseEntity.status(401).body(ErrorResponse.of(e.getMessage()));
		}
	}

	@PostMapping("/logout")
	@PreAuthorize("isAuthenticated()")
	@SecurityRequirement(name = "BearerAuth")
	@Operation(summary = "Logout", description = "Logout current user")
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
	public ResponseEntity<?> getUsers() {
		logger.info("GET /api/auth/users - Get all users called");
		try {
			List<UserResponse> users = authService.getAllUsers();
			Map<String, Object> response = new HashMap<>();
			response.put("users", users);
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			logger.error("GET /api/auth/users - Failed to retrieve users: {}", e.getMessage());
			return ResponseEntity.status(500).body(ErrorResponse.of(e.getMessage()));
		}
	}

	@PostMapping("/users")
	@PreAuthorize("hasRole('ADMIN')")
	@SecurityRequirement(name = "BearerAuth")
	@Operation(summary = "Create user", description = "Create a new user (admin only)")
	public ResponseEntity<?> createUser(@Valid @RequestBody CreateUserRequest request) {
		logger.info("POST /api/auth/users - Create user called with email={}, role={}", request.getEmail(),
				request.getRole());
		try {
			UserResponse user = authService.createUser(request.getEmail(), request.getFirstName(),
					request.getLastName(), request.getRole(), request.getPassword());
			logger.info("POST /api/auth/users - User created with id={}", user.getId());
			Map<String, Object> response = new HashMap<>();
			response.put("user", user);
			return ResponseEntity.status(201).body(response);
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
	public ResponseEntity<?> updateUser(@PathVariable UUID userId, @Valid @RequestBody UpdateUserRequest request) {
		logger.info("PUT /api/auth/users/{} - Update user called", userId);
		try {
			UserResponse user = authService.updateUser(userId, request.getEmail(), request.getFirstName(),
					request.getLastName(), request.getRole(), request.getPassword());
			logger.info("PUT /api/auth/users/{} - User updated successfully", userId);
			Map<String, Object> response = new HashMap<>();
			response.put("user", user);
			return ResponseEntity.ok(response);
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
	public ResponseEntity<?> deleteUser(@PathVariable UUID userId, HttpServletRequest httpRequest) {
		logger.info("DELETE /api/auth/users/{} - Delete user called", userId);
		try {
			UUID currentUserId = (UUID) httpRequest.getAttribute("userId");
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
	public ResponseEntity<?> updateProfile(@Valid @RequestBody UpdateProfileRequest request,
			HttpServletRequest httpRequest) {
		logger.info("PUT /api/auth/me - Update profile called");
		try {
			UUID userId = (UUID) httpRequest.getAttribute("userId");
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
	public ResponseEntity<?> deleteAccount(HttpServletRequest httpRequest) {
		logger.info("DELETE /api/auth/me - Delete account called");
		try {
			UUID userId = (UUID) httpRequest.getAttribute("userId");
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
