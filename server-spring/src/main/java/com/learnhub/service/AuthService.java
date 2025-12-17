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
import java.util.List;
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
            String accessToken = jwtUtil.generateToken(user.getEmail(), user.getId(), user.getRole().name());
            String refreshToken = jwtUtil.generateRefreshToken(user.getEmail(), user.getId(), user.getRole().name());
            return new LoginResponse(accessToken, refreshToken, mapToUserResponse(user));
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

        // Generate JWT tokens
        String accessToken = jwtUtil.generateToken(user.getEmail(), user.getId(), user.getRole().name());
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail(), user.getId(), user.getRole().name());
        return new LoginResponse(accessToken, refreshToken, mapToUserResponse(user));
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

    public UserResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        return mapToUserResponse(user);
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
            .stream()
            .map(this::mapToUserResponse)
            .toList();
    }

    @Transactional
    public UserResponse createUser(String email, String firstName, String lastName, String roleStr, String password) {
        // Check if user already exists
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("User with this email already exists");
        }

        UserRole role = UserRole.valueOf(roleStr);

        User user = new User();
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setRole(role);
        
        // Set password (required for both admin and teacher)
        if (password != null && !password.isEmpty()) {
            user.setPasswordHash(passwordEncoder.encode(password));
        }
        
        user = userRepository.save(user);

        // Send credentials email for teachers
        if (role == UserRole.TEACHER) {
            emailService.sendTeacherCredentials(email, firstName, password);
        }

        return mapToUserResponse(user);
    }

    @Transactional
    public UserResponse updateUser(Long userId, String email, String firstName, String lastName, String roleStr, String password) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        // Update email if provided
        if (email != null && !email.isEmpty() && !email.equals(user.getEmail())) {
            // Check if email is already taken by another user
            Optional<User> existingUser = userRepository.findByEmail(email);
            if (existingUser.isPresent() && !existingUser.get().getId().equals(userId)) {
                throw new RuntimeException("Email already exists");
            }
            user.setEmail(email);
        }

        // Update first name if provided
        if (firstName != null && !firstName.isEmpty()) {
            user.setFirstName(firstName);
        }

        // Update last name if provided
        if (lastName != null && !lastName.isEmpty()) {
            user.setLastName(lastName);
        }

        // Update role if provided
        if (roleStr != null && !roleStr.isEmpty()) {
            user.setRole(UserRole.valueOf(roleStr));
        }

        // Update password if provided
        if (password != null && !password.isEmpty()) {
            user.setPasswordHash(passwordEncoder.encode(password));
        }

        user = userRepository.save(user);
        return mapToUserResponse(user);
    }

    @Transactional
    public boolean deleteUser(Long userId, Long currentUserId) {
        // Prevent admin from deleting themselves
        if (userId.equals(currentUserId)) {
            throw new RuntimeException("Cannot delete your own account");
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return false;
        }

        // Delete related data
        verificationCodeRepository.deleteByUserId(userId);
        
        userRepository.delete(user);
        return true;
    }

    @Transactional
    public UserResponse updateProfile(Long userId, String email, String firstName, String lastName, String password) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        // Update email if provided
        if (email != null && !email.isEmpty() && !email.equals(user.getEmail())) {
            // Check if email is already taken by another user
            Optional<User> existingUser = userRepository.findByEmail(email);
            if (existingUser.isPresent() && !existingUser.get().getId().equals(userId)) {
                throw new RuntimeException("Email already exists");
            }
            user.setEmail(email);
        }

        // Update first name if provided
        if (firstName != null && !firstName.isEmpty()) {
            user.setFirstName(firstName);
        }

        // Update last name if provided
        if (lastName != null && !lastName.isEmpty()) {
            user.setLastName(lastName);
        }

        // Update password if provided
        if (password != null && !password.isEmpty()) {
            user.setPasswordHash(passwordEncoder.encode(password));
        }

        user = userRepository.save(user);
        return mapToUserResponse(user);
    }

    @Transactional
    public boolean deleteAccount(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return false;
        }

        // Delete related data
        verificationCodeRepository.deleteByUserId(userId);
        
        userRepository.delete(user);
        return true;
    }
    
    public LoginResponse refreshToken(String refreshToken) {
        // Validate refresh token
        if (!jwtUtil.validateRefreshToken(refreshToken)) {
            throw new RuntimeException("Invalid or expired refresh token");
        }
        
        // Extract user information from refresh token
        Long userId = jwtUtil.extractUserId(refreshToken);
        String email = jwtUtil.extractUsername(refreshToken);
        String role = jwtUtil.extractRole(refreshToken);
        
        // Verify user still exists
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Generate new tokens
        String newAccessToken = jwtUtil.generateToken(email, userId, role);
        String newRefreshToken = jwtUtil.generateRefreshToken(email, userId, role);
        
        return new LoginResponse(newAccessToken, newRefreshToken, mapToUserResponse(user));
    }
}
