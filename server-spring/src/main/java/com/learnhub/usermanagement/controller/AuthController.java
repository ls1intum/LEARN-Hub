package com.learnhub.usermanagement.controller;

import com.learnhub.usermanagement.dto.request.CreateUserRequest;
import com.learnhub.usermanagement.dto.request.LoginRequest;
import com.learnhub.usermanagement.dto.request.PasswordResetRequest;
import com.learnhub.usermanagement.dto.request.RefreshTokenRequest;
import com.learnhub.usermanagement.dto.request.TeacherRegistrationRequest;
import com.learnhub.usermanagement.dto.request.UpdateProfileRequest;
import com.learnhub.usermanagement.dto.request.UpdateUserRequest;
import com.learnhub.usermanagement.dto.request.VerifyCodeRequest;
import com.learnhub.dto.response.ErrorResponse;
import com.learnhub.usermanagement.dto.response.LoginResponse;
import com.learnhub.dto.response.MessageResponse;
import com.learnhub.usermanagement.dto.response.UserResponse;
import com.learnhub.usermanagement.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Authentication and user management endpoints")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register-teacher")
    @Operation(summary = "Register a new teacher", description = "Register a new teacher account and send verification code")
    public ResponseEntity<?> registerTeacher(@Valid @RequestBody TeacherRegistrationRequest request) {
        try {
            UserResponse user = authService.registerTeacher(request);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ErrorResponse.of(e.getMessage()));
        }
    }

    @PostMapping("/verification-code")
    @Operation(summary = "Request verification code", description = "Send a verification code to the user's email address")
    public ResponseEntity<?> requestVerificationCode(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            authService.requestVerificationCode(email);
            return ResponseEntity.ok(MessageResponse.of("Verification code sent"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ErrorResponse.of(e.getMessage()));
        }
    }

    @PostMapping("/verify")
    @Operation(summary = "Verify code and login", description = "Verify the code and complete login process")
    public ResponseEntity<?> verifyCode(@Valid @RequestBody VerifyCodeRequest request) {
        try {
            LoginResponse response = authService.verifyCode(request);
            Map<String, Object> result = new HashMap<>();
            result.put("user", response.getUser());
            result.put("access_token", response.getAccessToken());
            result.put("refresh_token", response.getRefreshToken());
            result.put("token_type", "Bearer");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ErrorResponse.of(e.getMessage()));
        }
    }

    @PostMapping("/login")
    @Operation(summary = "Login with password", description = "Login with email and password (admin or teacher)")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            LoginResponse response = authService.login(request);
            Map<String, Object> result = new HashMap<>();
            result.put("user", response.getUser());
            result.put("access_token", response.getAccessToken());
            result.put("refresh_token", response.getRefreshToken());
            result.put("token_type", "Bearer");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ErrorResponse.of(e.getMessage()));
        }
    }

    @PostMapping("/admin/login")
    @Operation(summary = "Admin login", description = "Login with admin credentials")
    public ResponseEntity<?> adminLogin(@Valid @RequestBody LoginRequest request) {
        try {
            LoginResponse response = authService.login(request);
            // Verify user is admin
            if (!"ADMIN".equals(response.getUser().getRole())) {
                return ResponseEntity.status(401).body(ErrorResponse.of("Unauthorized"));
            }
            Map<String, Object> result = new HashMap<>();
            result.put("user", response.getUser());
            result.put("access_token", response.getAccessToken());
            result.put("refresh_token", response.getRefreshToken());
            result.put("token_type", "Bearer");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ErrorResponse.of(e.getMessage()));
        }
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Get current user", description = "Get information about the currently authenticated user")
    public ResponseEntity<?> getCurrentUser(HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(401).body(ErrorResponse.of("Unauthorized"));
            }
            UserResponse user = authService.getUserById(userId);
            if (user == null) {
                return ResponseEntity.status(404).body(ErrorResponse.of("User not found"));
            }
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ErrorResponse.of(e.getMessage()));
        }
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh token", description = "Refresh the JWT access token using refresh token")
    public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenRequest request) {
        try {
            LoginResponse response = authService.refreshToken(request.getRefreshToken());
            Map<String, String> result = new HashMap<>();
            result.put("access_token", response.getAccessToken());
            result.put("refresh_token", response.getRefreshToken());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(ErrorResponse.of(e.getMessage()));
        }
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Logout", description = "Logout current user")
    public ResponseEntity<?> logout() {
        try {
            return ResponseEntity.ok(MessageResponse.of("Logged out successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ErrorResponse.of(e.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password", description = "Reset password for a teacher")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        try {
            authService.resetPassword(request.getEmail());
            return ResponseEntity.ok(MessageResponse.of("Password reset successfully. New credentials have been sent via email."));
        } catch (RuntimeException e) {
            if (e.getMessage().equals("Teacher not found")) {
                return ResponseEntity.status(404).body(ErrorResponse.of("Teacher not found"));
            }
            return ResponseEntity.badRequest().body(ErrorResponse.of(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ErrorResponse.of("Failed to reset password: " + e.getMessage()));
        }
    }

    // User management endpoints (admin only)
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Get all users", description = "Get list of all users (admin only)")
    public ResponseEntity<?> getUsers() {
        try {
            List<UserResponse> users = authService.getAllUsers();
            Map<String, Object> response = new HashMap<>();
            response.put("users", users);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ErrorResponse.of(e.getMessage()));
        }
    }

    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Create user", description = "Create a new user (admin only)")
    public ResponseEntity<?> createUser(@Valid @RequestBody CreateUserRequest request) {
        try {
            UserResponse user = authService.createUser(
                    request.getEmail(),
                    request.getFirstName(),
                    request.getLastName(),
                    request.getRole(),
                    request.getPassword());
            Map<String, Object> response = new HashMap<>();
            response.put("user", user);
            return ResponseEntity.status(201).body(response);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("already exists")) {
                return ResponseEntity.status(409).body(ErrorResponse.of(e.getMessage()));
            }
            return ResponseEntity.badRequest().body(ErrorResponse.of(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ErrorResponse.of(e.getMessage()));
        }
    }

    @PutMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Update user", description = "Update user details (admin only)")
    public ResponseEntity<?> updateUser(@PathVariable Long userId, @Valid @RequestBody UpdateUserRequest request) {
        try {
            UserResponse user = authService.updateUser(
                    userId,
                    request.getEmail(),
                    request.getFirstName(),
                    request.getLastName(),
                    request.getRole(),
                    request.getPassword());
            Map<String, Object> response = new HashMap<>();
            response.put("user", user);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(404).body(ErrorResponse.of(e.getMessage()));
            }
            if (e.getMessage().contains("already exists")) {
                return ResponseEntity.status(409).body(ErrorResponse.of(e.getMessage()));
            }
            return ResponseEntity.badRequest().body(ErrorResponse.of(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ErrorResponse.of(e.getMessage()));
        }
    }

    @DeleteMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Delete user", description = "Delete a user (admin only)")
    public ResponseEntity<?> deleteUser(@PathVariable Long userId, HttpServletRequest httpRequest) {
        try {
            Long currentUserId = (Long) httpRequest.getAttribute("userId");
            boolean deleted = authService.deleteUser(userId, currentUserId);
            if (!deleted) {
                return ResponseEntity.status(404).body(ErrorResponse.of("User not found"));
            }
            return ResponseEntity.ok(MessageResponse.of("User deleted successfully"));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("Cannot delete")) {
                return ResponseEntity.badRequest().body(ErrorResponse.of(e.getMessage()));
            }
            return ResponseEntity.badRequest().body(ErrorResponse.of(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ErrorResponse.of(e.getMessage()));
        }
    }

    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Update profile", description = "Update current user's profile")
    public ResponseEntity<?> updateProfile(@Valid @RequestBody UpdateProfileRequest request,
            HttpServletRequest httpRequest) {
        try {
            Long userId = (Long) httpRequest.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(401).body(ErrorResponse.of("Unauthorized"));
            }
            UserResponse user = authService.updateProfile(
                    userId,
                    request.getEmail(),
                    request.getFirstName(),
                    request.getLastName(),
                    request.getPassword());
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(404).body(ErrorResponse.of(e.getMessage()));
            }
            if (e.getMessage().contains("already exists")) {
                return ResponseEntity.status(409).body(ErrorResponse.of(e.getMessage()));
            }
            return ResponseEntity.badRequest().body(ErrorResponse.of(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ErrorResponse.of(e.getMessage()));
        }
    }

    @DeleteMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Delete account", description = "Delete current user's account")
    public ResponseEntity<?> deleteAccount(HttpServletRequest httpRequest) {
        try {
            Long userId = (Long) httpRequest.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(401).body(ErrorResponse.of("Unauthorized"));
            }
            boolean deleted = authService.deleteAccount(userId);
            if (!deleted) {
                return ResponseEntity.status(404).body(ErrorResponse.of("User not found"));
            }
            return ResponseEntity.ok(MessageResponse.of("Account deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ErrorResponse.of(e.getMessage()));
        }
    }
}
