package com.learnhub.usermanagement.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnhub.security.AuthenticatedUser;
import com.learnhub.security.SessionAuthenticationService;
import com.learnhub.usermanagement.dto.response.UserResponse;
import com.learnhub.usermanagement.entity.User;
import com.learnhub.usermanagement.service.AuthService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AuthControllerTest {

	private MockMvc mockMvc;
	private AuthController controller;
	private AuthService authService;
	private SessionAuthenticationService sessionAuthenticationService;
	private RememberMeServices rememberMeServices;
	private final ObjectMapper objectMapper = new ObjectMapper();

	@BeforeEach
	void setUp() {
		controller = new AuthController();
		authService = mock(AuthService.class);
		sessionAuthenticationService = mock(SessionAuthenticationService.class);
		rememberMeServices = mock(RememberMeServices.class);
		ReflectionTestUtils.setField(controller, "authService", authService);
		ReflectionTestUtils.setField(controller, "sessionAuthenticationService", sessionAuthenticationService);
		ReflectionTestUtils.setField(controller, "rememberMeServices", rememberMeServices);
		mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
	}

	private UserResponse sampleUser(UUID id, String role) {
		return new UserResponse(id, "teacher@example.com", "Ada", "Lovelace", role);
	}

	private Authentication principal(UUID userId, String role) {
		AuthenticatedUser authenticatedUser = new AuthenticatedUser(userId, "teacher@example.com", role);
		return new UsernamePasswordAuthenticationToken(authenticatedUser, null,
				List.of(new SimpleGrantedAuthority("ROLE_" + role)));
	}

	// ─── register-teacher ───────────────────────────────────────────

	@Test
	void registerTeacherReturns201WithCreatedUser() throws Exception {
		UUID id = UUID.randomUUID();
		when(authService.registerTeacher(any())).thenReturn(sampleUser(id, "TEACHER"));

		mockMvc.perform(post("/api/auth/register-teacher").contentType("application/json")
				.content(objectMapper.writeValueAsString(
						Map.of("email", "teacher@example.com", "firstName", "Ada", "lastName", "Lovelace"))))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.user.id").value(id.toString()))
				.andExpect(jsonPath("$.message").exists());
	}

	@Test
	void registerTeacherRejectsInvalidEmail() throws Exception {
		mockMvc.perform(post("/api/auth/register-teacher").contentType("application/json")
				.content(objectMapper.writeValueAsString(
						Map.of("email", "not-an-email", "firstName", "Ada", "lastName", "Lovelace"))))
				.andExpect(status().isBadRequest());
	}

	@Test
	void registerTeacherReturns400WhenServiceFails() throws Exception {
		when(authService.registerTeacher(any())).thenThrow(new RuntimeException("Email already registered"));

		mockMvc.perform(post("/api/auth/register-teacher").contentType("application/json")
				.content(objectMapper.writeValueAsString(Map.of("email", "teacher@example.com", "firstName", "Ada",
						"lastName", "Lovelace"))))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("Email already registered"));
	}

	// ─── verification-code ──────────────────────────────────────────

	@Test
	void requestVerificationCodeReturns200() throws Exception {
		mockMvc.perform(post("/api/auth/verification-code").contentType("application/json")
				.content(objectMapper.writeValueAsString(Map.of("email", "teacher@example.com"))))
				.andExpect(status().isOk()).andExpect(jsonPath("$.message").value("Verification code sent"));
	}

	@Test
	void requestVerificationCodeReturns400OnFailure() throws Exception {
		org.mockito.Mockito.doThrow(new RuntimeException("Unknown email")).when(authService)
				.requestVerificationCode(any());

		mockMvc.perform(post("/api/auth/verification-code").contentType("application/json")
				.content(objectMapper.writeValueAsString(Map.of("email", "ghost@example.com"))))
				.andExpect(status().isBadRequest());
	}

	// ─── verify ─────────────────────────────────────────────────────

	@Test
	void verifyCodeReturns200WithUserEnvelope() throws Exception {
		UUID id = UUID.randomUUID();
		User user = mock(User.class);
		when(user.getId()).thenReturn(id);
		when(authService.verifyCode(any())).thenReturn(user);
		when(sessionAuthenticationService.createAuthentication(user)).thenReturn(principal(id, "TEACHER"));
		when(authService.getUserById(id)).thenReturn(sampleUser(id, "TEACHER"));

		mockMvc.perform(post("/api/auth/verify").contentType("application/json")
				.content(objectMapper.writeValueAsString(Map.of("email", "teacher@example.com", "code", "123456"))))
				.andExpect(status().isOk()).andExpect(jsonPath("$.user.id").value(id.toString()));
	}

	@Test
	void verifyCodeRejectsMissingCode() throws Exception {
		mockMvc.perform(post("/api/auth/verify").contentType("application/json")
				.content(objectMapper.writeValueAsString(Map.of("email", "teacher@example.com"))))
				.andExpect(status().isBadRequest());
	}

	@Test
	void verifyCodeReturns400OnInvalidCode() throws Exception {
		when(authService.verifyCode(any())).thenThrow(new RuntimeException("Invalid code"));

		mockMvc.perform(post("/api/auth/verify").contentType("application/json").content(
				objectMapper.writeValueAsString(Map.of("email", "teacher@example.com", "code", "000000"))))
				.andExpect(status().isBadRequest());
	}

	// ─── login ──────────────────────────────────────────────────────

	@Test
	void loginReturns200WithUserEnvelope() throws Exception {
		UUID id = UUID.randomUUID();
		User user = mock(User.class);
		when(user.getId()).thenReturn(id);
		when(authService.login(any())).thenReturn(user);
		when(sessionAuthenticationService.createAuthentication(user)).thenReturn(principal(id, "ADMIN"));
		when(authService.getUserById(id)).thenReturn(sampleUser(id, "ADMIN"));

		mockMvc.perform(post("/api/auth/login").contentType("application/json").content(
				objectMapper.writeValueAsString(Map.of("email", "teacher@example.com", "password", "password123"))))
				.andExpect(status().isOk()).andExpect(jsonPath("$.user.role").value("ADMIN"));
	}

	@Test
	void loginReturns400OnBadCredentials() throws Exception {
		when(authService.login(any())).thenThrow(new RuntimeException("Invalid credentials"));

		mockMvc.perform(post("/api/auth/login").contentType("application/json").content(
				objectMapper.writeValueAsString(Map.of("email", "teacher@example.com", "password", "wrong"))))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("Invalid credentials"));
	}

	@Test
	void loginRejectsInvalidEmail() throws Exception {
		mockMvc.perform(post("/api/auth/login").contentType("application/json")
				.content(objectMapper.writeValueAsString(Map.of("email", "nope", "password", "password123"))))
				.andExpect(status().isBadRequest());
	}

	// ─── /me ────────────────────────────────────────────────────────

	@Test
	void getCurrentUserReturns200WhenAuthenticated() throws Exception {
		UUID id = UUID.randomUUID();
		when(authService.getUserById(id)).thenReturn(sampleUser(id, "TEACHER"));

		mockMvc.perform(get("/api/auth/me").principal(principal(id, "TEACHER"))).andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id.toString()));
	}

	@Test
	void getCurrentUserReturns401WhenNoPrincipal() throws Exception {
		mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
	}

	@Test
	void getCurrentUserReturns401WhenUserNoLongerExists() throws Exception {
		UUID id = UUID.randomUUID();
		when(authService.getUserById(id)).thenReturn(null);

		mockMvc.perform(get("/api/auth/me").principal(principal(id, "TEACHER"))).andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("Session invalid"));
	}

	// ─── csrf (direct call: CsrfToken is request-scoped) ────────────

	@Test
	void csrfReturnsToken() {
		CsrfToken token = mock(CsrfToken.class);
		when(token.getToken()).thenReturn("csrf-abc");

		ResponseEntity<?> response = controller.csrf(token);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isInstanceOf(Map.class);
		assertThat(((Map<?, ?>) response.getBody()).get("token")).isEqualTo("csrf-abc");
	}

	// ─── reset-password ─────────────────────────────────────────────

	@Test
	void resetPasswordReturns200() throws Exception {
		mockMvc.perform(post("/api/auth/reset-password").contentType("application/json")
				.content(objectMapper.writeValueAsString(Map.of("email", "teacher@example.com"))))
				.andExpect(status().isOk());
	}

	@Test
	void resetPasswordReturns404WhenTeacherNotFound() throws Exception {
		org.mockito.Mockito.doThrow(new RuntimeException("Teacher not found")).when(authService).resetPassword(any());

		mockMvc.perform(post("/api/auth/reset-password").contentType("application/json")
				.content(objectMapper.writeValueAsString(Map.of("email", "ghost@example.com"))))
				.andExpect(status().isNotFound());
	}

	@Test
	void resetPasswordReturns400OnOtherRuntimeError() throws Exception {
		org.mockito.Mockito.doThrow(new RuntimeException("Failed to send email")).when(authService)
				.resetPassword(any());

		mockMvc.perform(post("/api/auth/reset-password").contentType("application/json")
				.content(objectMapper.writeValueAsString(Map.of("email", "teacher@example.com"))))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("Failed to send email"));
	}

	// ─── admin: users CRUD ──────────────────────────────────────────

	@Test
	void getUsersReturnsList() throws Exception {
		when(authService.getAllUsers())
				.thenReturn(List.of(sampleUser(UUID.randomUUID(), "ADMIN"), sampleUser(UUID.randomUUID(), "TEACHER")));

		mockMvc.perform(get("/api/auth/users")).andExpect(status().isOk())
				.andExpect(jsonPath("$.users.length()").value(2));
	}

	@Test
	void createUserReturns201() throws Exception {
		UUID id = UUID.randomUUID();
		when(authService.createUser(any(), any(), any(), any(), any())).thenReturn(sampleUser(id, "TEACHER"));

		mockMvc.perform(post("/api/auth/users").contentType("application/json")
				.content(objectMapper.writeValueAsString(Map.of("email", "new@example.com", "firstName", "New",
						"lastName", "User", "role", "TEACHER", "password", "password123"))))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.user.id").value(id.toString()));
	}

	@Test
	void createUserReturns409WhenAlreadyExists() throws Exception {
		when(authService.createUser(any(), any(), any(), any(), any()))
				.thenThrow(new RuntimeException("User already exists"));

		mockMvc.perform(post("/api/auth/users").contentType("application/json")
				.content(objectMapper.writeValueAsString(Map.of("email", "dup@example.com", "firstName", "Dup",
						"lastName", "User", "role", "ADMIN", "password", "password123"))))
				.andExpect(status().isConflict());
	}

	@Test
	void createUserRejectsInvalidRole() throws Exception {
		mockMvc.perform(post("/api/auth/users").contentType("application/json")
				.content(objectMapper.writeValueAsString(Map.of("email", "new@example.com", "firstName", "New",
						"lastName", "User", "role", "SUPERUSER", "password", "password123"))))
				.andExpect(status().isBadRequest());
	}

	@Test
	void updateUserReturns200() throws Exception {
		UUID id = UUID.randomUUID();
		when(authService.updateUser(eq(id), any(), any(), any(), any(), any())).thenReturn(sampleUser(id, "ADMIN"));

		mockMvc.perform(put("/api/auth/users/" + id).contentType("application/json")
				.content(objectMapper.writeValueAsString(Map.of("firstName", "Edited", "role", "ADMIN"))))
				.andExpect(status().isOk()).andExpect(jsonPath("$.user.id").value(id.toString()));
	}

	@Test
	void updateUserReturns404WhenNotFound() throws Exception {
		UUID id = UUID.randomUUID();
		when(authService.updateUser(eq(id), any(), any(), any(), any(), any()))
				.thenThrow(new RuntimeException("User not found"));

		mockMvc.perform(put("/api/auth/users/" + id).contentType("application/json")
				.content(objectMapper.writeValueAsString(Map.of("firstName", "Edited"))))
				.andExpect(status().isNotFound());
	}

	@Test
	void updateUserReturns409OnEmailConflict() throws Exception {
		UUID id = UUID.randomUUID();
		when(authService.updateUser(eq(id), any(), any(), any(), any(), any()))
				.thenThrow(new RuntimeException("Email already exists"));

		mockMvc.perform(put("/api/auth/users/" + id).contentType("application/json")
				.content(objectMapper.writeValueAsString(Map.of("email", "taken@example.com"))))
				.andExpect(status().isConflict());
	}

	@Test
	void deleteUserReturns200() throws Exception {
		UUID id = UUID.randomUUID();
		UUID adminId = UUID.randomUUID();
		when(authService.deleteUser(eq(id), eq(adminId))).thenReturn(true);

		mockMvc.perform(delete("/api/auth/users/" + id).principal(principal(adminId, "ADMIN")))
				.andExpect(status().isOk());
	}

	@Test
	void deleteUserReturns404WhenMissing() throws Exception {
		UUID id = UUID.randomUUID();
		UUID adminId = UUID.randomUUID();
		when(authService.deleteUser(eq(id), eq(adminId))).thenReturn(false);

		mockMvc.perform(delete("/api/auth/users/" + id).principal(principal(adminId, "ADMIN")))
				.andExpect(status().isNotFound());
	}

	@Test
	void deleteUserReturns401WhenNoPrincipal() throws Exception {
		mockMvc.perform(delete("/api/auth/users/" + UUID.randomUUID())).andExpect(status().isUnauthorized());
	}

	// ─── profile (self) ─────────────────────────────────────────────

	@Test
	void updateProfileReturns200() throws Exception {
		UUID id = UUID.randomUUID();
		when(authService.updateProfile(eq(id), any(), any(), any(), any())).thenReturn(sampleUser(id, "TEACHER"));

		mockMvc.perform(put("/api/auth/me").principal(principal(id, "TEACHER")).contentType("application/json")
				.content(objectMapper.writeValueAsString(Map.of("firstName", "Renamed")))).andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id.toString()));
	}

	@Test
	void updateProfileReturns401WhenNoPrincipal() throws Exception {
		mockMvc.perform(put("/api/auth/me").contentType("application/json")
				.content(objectMapper.writeValueAsString(Map.of("firstName", "Renamed"))))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void deleteAccountReturns200() throws Exception {
		UUID id = UUID.randomUUID();
		when(authService.deleteAccount(id)).thenReturn(true);

		mockMvc.perform(delete("/api/auth/me").principal(principal(id, "TEACHER"))).andExpect(status().isOk());
	}

	@Test
	void deleteAccountReturns404WhenMissing() throws Exception {
		UUID id = UUID.randomUUID();
		when(authService.deleteAccount(id)).thenReturn(false);

		mockMvc.perform(delete("/api/auth/me").principal(principal(id, "TEACHER"))).andExpect(status().isNotFound());
	}

	@Test
	void deleteAccountReturns401WhenNoPrincipal() throws Exception {
		mockMvc.perform(delete("/api/auth/me")).andExpect(status().isUnauthorized());
	}
}
