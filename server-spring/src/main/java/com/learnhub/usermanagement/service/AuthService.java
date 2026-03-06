package com.learnhub.usermanagement.service;

import com.learnhub.exception.BadRequestException;
import com.learnhub.exception.ConflictException;
import com.learnhub.exception.EntityNotFoundException;
import com.learnhub.security.JwtUtil;
import com.learnhub.usermanagement.dto.request.LoginRequest;
import com.learnhub.usermanagement.dto.request.TeacherRegistrationRequest;
import com.learnhub.usermanagement.dto.request.VerifyCodeRequest;
import com.learnhub.usermanagement.dto.response.LoginResponse;
import com.learnhub.usermanagement.dto.response.UserResponse;
import com.learnhub.usermanagement.entity.User;
import com.learnhub.usermanagement.entity.VerificationCode;
import com.learnhub.usermanagement.entity.enums.UserRole;
import com.learnhub.usermanagement.repository.UserRepository;
import com.learnhub.usermanagement.repository.VerificationCodeRepository;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

	private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private final UserRepository userRepository;
	private final VerificationCodeRepository verificationCodeRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtUtil jwtUtil;
	private final EmailService emailService;

	public AuthService(UserRepository userRepository, VerificationCodeRepository verificationCodeRepository,
			PasswordEncoder passwordEncoder, JwtUtil jwtUtil, EmailService emailService) {
		this.userRepository = userRepository;
		this.verificationCodeRepository = verificationCodeRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtUtil = jwtUtil;
		this.emailService = emailService;
	}

	@Transactional
	public UserResponse registerTeacher(TeacherRegistrationRequest request) {
		if (userRepository.existsByEmail(request.getEmail())) {
			throw new BadRequestException("Email already registered");
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

	public LoginResponse login(LoginRequest request) {
		User user = userRepository.findByEmail(request.getEmail())
				.orElseThrow(() -> new EntityNotFoundException("User not found"));

		// For teachers without password, generate and send verification code
		if (user.getPasswordHash() == null) {
			String code = generateVerificationCode();
			saveVerificationCode(user.getId(), code);
			emailService.sendVerificationCode(user.getEmail(), code, user.getFirstName());
			throw new BadRequestException("Verification code sent to your email");
		}

		// For admin users with password
		if (request.getPassword() != null && passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
			String accessToken = jwtUtil.generateToken(user.getEmail(), user.getId(), user.getRole().name());
			String refreshToken = jwtUtil.generateRefreshToken(user.getEmail(), user.getId(), user.getRole().name());
			return new LoginResponse(accessToken, refreshToken, mapToUserResponse(user));
		}

		throw new BadRequestException("Invalid credentials");
	}

	@Transactional
	public LoginResponse verifyCode(VerifyCodeRequest request) {
		User user = userRepository.findByEmail(request.getEmail())
				.orElseThrow(() -> new EntityNotFoundException("User not found"));

		VerificationCode verificationCode = verificationCodeRepository
				.findByUserIdAndCodeAndUsedAndExpiresAtAfter(user.getId(), request.getCode(), "N", LocalDateTime.now())
				.orElseThrow(() -> new BadRequestException("Invalid or expired verification code"));

		// Mark code as used
		verificationCode.setUsed("Y");
		verificationCodeRepository.save(verificationCode);

		// Generate JWT tokens
		String accessToken = jwtUtil.generateToken(user.getEmail(), user.getId(), user.getRole().name());
		String refreshToken = jwtUtil.generateRefreshToken(user.getEmail(), user.getId(), user.getRole().name());
		return new LoginResponse(accessToken, refreshToken, mapToUserResponse(user));
	}

	public void requestVerificationCode(String email) {
		User user = userRepository.findByEmail(email).orElseThrow(() -> new EntityNotFoundException("User not found"));

		String code = generateVerificationCode();
		saveVerificationCode(user.getId(), code);
		emailService.sendVerificationCode(user.getEmail(), code, user.getFirstName());
	}

	private String generateVerificationCode() {
		return String.format("%06d", SECURE_RANDOM.nextInt(1000000));
	}

	@Transactional
	private void saveVerificationCode(UUID userId, String code) {
		// Delete old codes for this user
		deleteAllVerificationCodesForUser(userId);

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

	public UserResponse getUserById(UUID userId) {
		User user = userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("User not found"));
		return mapToUserResponse(user);
	}

	public List<UserResponse> getAllUsers() {
		return userRepository.findAll().stream().map(this::mapToUserResponse).toList();
	}

	@Transactional
	public UserResponse createUser(String email, String firstName, String lastName, String roleStr, String password) {
		// Check if user already exists
		if (userRepository.existsByEmail(email)) {
			throw new ConflictException("User with this email already exists");
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
	public UserResponse updateUser(UUID userId, String email, String firstName, String lastName, String roleStr,
			String password) {
		User user = userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("User not found"));

		// Update email if provided
		if (email != null && !email.isEmpty() && !email.equals(user.getEmail())) {
			// Check if email is already taken by another user
			Optional<User> existingUser = userRepository.findByEmail(email);
			if (existingUser.isPresent() && !existingUser.get().getId().equals(userId)) {
				throw new ConflictException("Email already exists");
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
	public boolean deleteUser(UUID userId, UUID currentUserId) {
		// Prevent admin from deleting themselves
		if (userId.equals(currentUserId)) {
			throw new BadRequestException("Cannot delete your own account");
		}

		User user = userRepository.findById(userId).orElse(null);
		if (user == null) {
			return false;
		}

		// Delete related data
		deleteAllVerificationCodesForUser(userId);

		userRepository.delete(user);
		return true;
	}

	@Transactional
	public UserResponse updateProfile(UUID userId, String email, String firstName, String lastName, String password) {
		User user = userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("User not found"));

		// Update email if provided
		if (email != null && !email.isEmpty() && !email.equals(user.getEmail())) {
			// Check if email is already taken by another user
			Optional<User> existingUser = userRepository.findByEmail(email);
			if (existingUser.isPresent() && !existingUser.get().getId().equals(userId)) {
				throw new ConflictException("Email already exists");
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
	public boolean deleteAccount(UUID userId) {
		User user = userRepository.findById(userId).orElse(null);
		if (user == null) {
			return false;
		}

		// Delete related data
		deleteAllVerificationCodesForUser(userId);

		userRepository.delete(user);
		return true;
	}

	public LoginResponse refreshToken(String refreshToken) {
		// Validate refresh token
		if (!jwtUtil.validateRefreshToken(refreshToken)) {
			throw new BadRequestException("Invalid or expired refresh token");
		}

		// Extract user information from refresh token
		UUID userId = jwtUtil.extractUserId(refreshToken);
		String email = jwtUtil.extractUsername(refreshToken);
		String role = jwtUtil.extractRole(refreshToken);

		// Verify user still exists
		User user = userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("User not found"));

		// Generate new tokens
		String newAccessToken = jwtUtil.generateToken(email, userId, role);
		String newRefreshToken = jwtUtil.generateRefreshToken(email, userId, role);

		return new LoginResponse(newAccessToken, newRefreshToken, mapToUserResponse(user));
	}

	@Transactional
	public void resetPassword(String email) {
		// Find user by email
		User user = userRepository.findByEmail(email).orElseThrow(() -> new EntityNotFoundException("Teacher not found"));

		// Verify user is a teacher (not admin)
		if (user.getRole() != UserRole.TEACHER) {
			throw new EntityNotFoundException("Teacher not found");
		}

		// Generate new secure password (similar to Flask's PasswordGenerator)
		String newPassword = generateSecurePassword();

		// Set new password
		user.setPasswordHash(passwordEncoder.encode(newPassword));
		userRepository.save(user);

		// Send password reset email
		try {
			emailService.sendPasswordReset(user.getEmail(), user.getFirstName(), newPassword);
		} catch (Exception e) {
			// Log warning but don't fail - password was already reset
			logger.warn("Failed to send password reset email to {}, but password was reset", email);
		}
	}

	private String generateSecurePassword() {
		// Generate a secure random password similar to Flask's PasswordGenerator
		// Format: 3 random words + 2 digits + 1 special character
		String[] words = {"happy", "sunny", "bright", "swift", "clear", "fresh", "quick", "smart", "brave", "calm",
				"kind", "wise", "proud", "strong", "gentle", "bold"};

		StringBuilder password = new StringBuilder();

		// Add 3 random words with first letter capitalized
		for (int i = 0; i < 3; i++) {
			String word = words[SECURE_RANDOM.nextInt(words.length)];
			password.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
		}

		// Add 2 random digits
		password.append(SECURE_RANDOM.nextInt(10));
		password.append(SECURE_RANDOM.nextInt(10));

		// Add 1 special character
		char[] specialChars = {'!', '@', '#', '$', '%', '&', '*'};
		password.append(specialChars[SECURE_RANDOM.nextInt(specialChars.length)]);

		return password.toString();
	}

	@Transactional
	private void deleteAllVerificationCodesForUser(UUID userId) {
		List<VerificationCode> existingCodes = verificationCodeRepository.findByUserId(userId);
		if (!existingCodes.isEmpty()) {
			verificationCodeRepository.deleteAll(existingCodes);
		}
	}
}
