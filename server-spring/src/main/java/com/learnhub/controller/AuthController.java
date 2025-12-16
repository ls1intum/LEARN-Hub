package com.learnhub.controller;

import com.learnhub.dto.request.LoginRequest;
import com.learnhub.dto.request.TeacherRegistrationRequest;
import com.learnhub.dto.request.VerifyCodeRequest;
import com.learnhub.dto.response.ApiResponse;
import com.learnhub.dto.response.LoginResponse;
import com.learnhub.dto.response.UserResponse;
import com.learnhub.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Authentication and user management endpoints")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a new teacher", description = "Register a new teacher account and send verification code")
    public ResponseEntity<ApiResponse<UserResponse>> registerTeacher(@Valid @RequestBody TeacherRegistrationRequest request) {
        try {
            UserResponse user = authService.registerTeacher(request);
            return ResponseEntity.ok(ApiResponse.success("Verification code sent to your email", user));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Login with email and password or request verification code")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        try {
            LoginResponse response = authService.login(request);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/verify-code")
    @Operation(summary = "Verify code", description = "Verify the code sent to email and get JWT token")
    public ResponseEntity<ApiResponse<LoginResponse>> verifyCode(@Valid @RequestBody VerifyCodeRequest request) {
        try {
            LoginResponse response = authService.verifyCode(request);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/request-verification-code")
    @Operation(summary = "Request verification code", description = "Request a new verification code")
    public ResponseEntity<ApiResponse<Void>> requestVerificationCode(@RequestParam String email) {
        try {
            authService.requestVerificationCode(email);
            return ResponseEntity.ok(ApiResponse.success("Verification code sent to your email", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
