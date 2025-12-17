package com.learnhub.controller;

import com.learnhub.dto.request.LoginRequest;
import com.learnhub.dto.request.TeacherRegistrationRequest;
import com.learnhub.dto.request.VerifyCodeRequest;
import com.learnhub.dto.response.ApiResponse;
import com.learnhub.dto.response.LoginResponse;
import com.learnhub.dto.response.UserResponse;
import com.learnhub.service.AuthService;
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
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Authentication and user management endpoints")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register-teacher")
    @Operation(summary = "Register a new teacher", description = "Register a new teacher account and send verification code")
    public ResponseEntity<ApiResponse<UserResponse>> registerTeacher(@Valid @RequestBody TeacherRegistrationRequest request) {
        try {
            UserResponse user = authService.registerTeacher(request);
            return ResponseEntity.ok(ApiResponse.success("Verification code sent to your email", user));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/verification-code")
    @Operation(summary = "Request verification code", description = "Send a verification code to the user's email address")
    public ResponseEntity<ApiResponse<Map<String, String>>> requestVerificationCode(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            authService.requestVerificationCode(email);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Verification code sent");
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/verify")
    @Operation(summary = "Verify code and login", description = "Verify the code and complete login process")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyCode(@Valid @RequestBody VerifyCodeRequest request) {
        try {
            LoginResponse response = authService.verifyCode(request);
            Map<String, Object> result = new HashMap<>();
            result.put("user", response.getUser());
            result.put("access_token", response.getAccessToken());
            result.put("refresh_token", response.getAccessToken()); // TODO: Implement refresh token
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/login")
    @Operation(summary = "Login with password", description = "Login with email and password (admin or teacher)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(@Valid @RequestBody LoginRequest request) {
        try {
            LoginResponse response = authService.login(request);
            Map<String, Object> result = new HashMap<>();
            result.put("user", response.getUser());
            result.put("access_token", response.getAccessToken());
            result.put("refresh_token", response.getAccessToken()); // TODO: Implement refresh token
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/admin/login")
    @Operation(summary = "Admin login", description = "Login with admin credentials")
    public ResponseEntity<ApiResponse<Map<String, Object>>> adminLogin(@Valid @RequestBody LoginRequest request) {
        try {
            LoginResponse response = authService.login(request);
            // Verify user is admin
            if (!"ADMIN".equals(response.getUser().getRole())) {
                return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized"));
            }
            Map<String, Object> result = new HashMap<>();
            result.put("user", response.getUser());
            result.put("access_token", response.getAccessToken());
            result.put("refresh_token", response.getAccessToken()); // TODO: Implement refresh token
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Get current user", description = "Get information about the currently authenticated user")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized"));
            }
            // TODO: Implement get user by ID
            UserResponse user = new UserResponse();
            user.setId(userId);
            return ResponseEntity.ok(ApiResponse.success(user));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh token", description = "Refresh the JWT access token using refresh token")
    public ResponseEntity<ApiResponse<Map<String, String>>> refreshToken(@RequestBody Map<String, String> request) {
        try {
            // TODO: Implement refresh token logic
            Map<String, String> response = new HashMap<>();
            response.put("access_token", "");
            response.put("refresh_token", "");
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Logout", description = "Logout current user")
    public ResponseEntity<ApiResponse<Map<String, String>>> logout() {
        try {
            Map<String, String> response = new HashMap<>();
            response.put("message", "Logged out successfully");
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password", description = "Request password reset")
    public ResponseEntity<ApiResponse<Map<String, String>>> resetPassword(@RequestBody Map<String, String> request) {
        try {
            // TODO: Implement password reset
            Map<String, String> response = new HashMap<>();
            response.put("message", "Password reset email sent");
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // User management endpoints (admin only)
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Get all users", description = "Get list of all users (admin only)")
    public ResponseEntity<ApiResponse<Object>> getUsers() {
        try {
            // TODO: Implement get all users
            return ResponseEntity.ok(ApiResponse.success(new HashMap<>()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Create user", description = "Create a new user (admin only)")
    public ResponseEntity<ApiResponse<Object>> createUser(@RequestBody Map<String, Object> request) {
        try {
            // TODO: Implement create user
            return ResponseEntity.ok(ApiResponse.success(new HashMap<>()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Update user", description = "Update user details (admin only)")
    public ResponseEntity<ApiResponse<Object>> updateUser(@PathVariable Long userId, @RequestBody Map<String, Object> request) {
        try {
            // TODO: Implement update user
            return ResponseEntity.ok(ApiResponse.success(new HashMap<>()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Delete user", description = "Delete a user (admin only)")
    public ResponseEntity<ApiResponse<Map<String, String>>> deleteUser(@PathVariable Long userId) {
        try {
            // TODO: Implement delete user
            Map<String, String> response = new HashMap<>();
            response.put("message", "User deleted successfully");
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Update profile", description = "Update current user's profile")
    public ResponseEntity<ApiResponse<Object>> updateProfile(@RequestBody Map<String, Object> request, HttpServletRequest httpRequest) {
        try {
            // TODO: Implement update profile
            return ResponseEntity.ok(ApiResponse.success(new HashMap<>()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Delete account", description = "Delete current user's account")
    public ResponseEntity<ApiResponse<Map<String, String>>> deleteAccount() {
        try {
            // TODO: Implement delete account
            Map<String, String> response = new HashMap<>();
            response.put("message", "Account deleted successfully");
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
