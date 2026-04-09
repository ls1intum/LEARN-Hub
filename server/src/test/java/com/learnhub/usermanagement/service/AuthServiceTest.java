package com.learnhub.usermanagement.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.learnhub.usermanagement.dto.request.VerifyCodeRequest;
import com.learnhub.usermanagement.entity.User;
import com.learnhub.usermanagement.entity.enums.UserRole;
import com.learnhub.usermanagement.repository.UserRepository;
import com.learnhub.usermanagement.repository.VerificationCodeRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

class AuthServiceTest {

	private AuthService authService;
	private UserRepository userRepository;
	private VerificationCodeRepository verificationCodeRepository;
	private EmailService emailService;

	@BeforeEach
	void setUp() {
		authService = new AuthService();
		userRepository = mock(UserRepository.class);
		verificationCodeRepository = mock(VerificationCodeRepository.class);
		emailService = mock(EmailService.class);

		ReflectionTestUtils.setField(authService, "userRepository", userRepository);
		ReflectionTestUtils.setField(authService, "verificationCodeRepository", verificationCodeRepository);
		ReflectionTestUtils.setField(authService, "emailService", emailService);
		ReflectionTestUtils.setField(authService, "passwordEncoder", mock(PasswordEncoder.class));
	}

	@Test
	void requestVerificationCodeRejectsAdminUsers() {
		User admin = createUser(UserRole.ADMIN);
		when(userRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));

		assertThatThrownBy(() -> authService.requestVerificationCode(admin.getEmail()))
				.isInstanceOf(RuntimeException.class).hasMessage("Admins must log in with password");

		verify(verificationCodeRepository, never()).save(org.mockito.ArgumentMatchers.any());
		verify(emailService, never()).sendVerificationCode(org.mockito.ArgumentMatchers.anyString(),
				org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
	}

	@Test
	void verifyCodeRejectsAdminUsers() {
		User admin = createUser(UserRole.ADMIN);
		when(userRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));

		VerifyCodeRequest request = new VerifyCodeRequest();
		request.setEmail(admin.getEmail());
		request.setCode("123456");

		assertThatThrownBy(() -> authService.verifyCode(request)).isInstanceOf(RuntimeException.class)
				.hasMessage("Admins must log in with password");

		verify(verificationCodeRepository, never()).findByUserIdAndCodeAndUsedAndExpiresAtAfter(
				org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString(),
				org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any());
	}

	private User createUser(UserRole role) {
		User user = new User();
		user.setId(UUID.randomUUID());
		user.setEmail(role.name().toLowerCase() + "@example.com");
		user.setFirstName("Test");
		user.setLastName("User");
		user.setRole(role);
		return user;
	}
}
