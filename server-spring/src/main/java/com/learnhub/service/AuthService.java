package com.learnhub.service;

import com.learnhub.dto.request.LoginRequest;
import com.learnhub.dto.request.TeacherRegistrationRequest;
import com.learnhub.dto.request.VerifyCodeRequest;
import com.learnhub.dto.response.LoginResponse;
import com.learnhub.dto.response.UserResponse;
import com.learnhub.model.User;
import com.learnhub.model.VerificationCode;
import com.learnhub.model.enums.UserRole;
import com.learnhub.repository.UserRepository;
import com.learnhub.repository.VerificationCodeRepository;
import com.learnhub.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VerificationCodeRepository verificationCodeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private EmailService emailService;

    @Transactional
    public UserResponse registerTeacher(TeacherRegistrationRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setRole(UserRole.TEACHER);
        user = userRepository.save(user);

        // Generate and send verification code
        String code = generateVerificationCode();
        saveVerificationCode(user.getId(), code);
        emailService.sendVerificationCode(user.getEmail(), code, user.getFirstName());

        return mapToUserResponse(user);
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new RuntimeException("User not found"));

        // For teachers without password, generate and send verification code
        if (user.getPasswordHash() == null) {
            String code = generateVerificationCode();
            saveVerificationCode(user.getId(), code);
            emailService.sendVerificationCode(user.getEmail(), code, user.getFirstName());
            throw new RuntimeException("Verification code sent to your email");
        }

        // For admin users with password
        if (request.getPassword() != null && passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            String token = jwtUtil.generateToken(user.getEmail(), user.getId(), user.getRole().name());
            return new LoginResponse(token, mapToUserResponse(user));
        }

        throw new RuntimeException("Invalid credentials");
    }

    @Transactional
    public LoginResponse verifyCode(VerifyCodeRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new RuntimeException("User not found"));

        VerificationCode verificationCode = verificationCodeRepository
            .findByUserIdAndCodeAndUsedAndExpiresAtAfter(
                user.getId(), request.getCode(), "N", LocalDateTime.now())
            .orElseThrow(() -> new RuntimeException("Invalid or expired verification code"));

        // Mark code as used
        verificationCode.setUsed("Y");
        verificationCodeRepository.save(verificationCode);

        // Generate JWT token
        String token = jwtUtil.generateToken(user.getEmail(), user.getId(), user.getRole().name());
        return new LoginResponse(token, mapToUserResponse(user));
    }

    @Transactional
    public void requestVerificationCode(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found"));

        String code = generateVerificationCode();
        saveVerificationCode(user.getId(), code);
        emailService.sendVerificationCode(user.getEmail(), code, user.getFirstName());
    }

    private String generateVerificationCode() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(1000000));
    }

    private void saveVerificationCode(Long userId, String code) {
        // Delete old codes for this user
        verificationCodeRepository.deleteByUserId(userId);

        VerificationCode verificationCode = new VerificationCode();
        verificationCode.setUserId(userId);
        verificationCode.setCode(code);
        verificationCode.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        verificationCode.setAttempts(0);
        verificationCode.setUsed("N");
        verificationCodeRepository.save(verificationCode);
    }

    private UserResponse mapToUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setRole(user.getRole().name());
        return response;
    }
}
