package com.learnhub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, CorsConfigurationSource corsConfigurationSource,
			RememberMeServices rememberMeServices) throws Exception {
		CsrfTokenRequestAttributeHandler csrfTokenRequestHandler = new CsrfTokenRequestAttributeHandler();
		csrfTokenRequestHandler.setCsrfRequestAttributeName("_csrf");

		http.csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
				.csrfTokenRequestHandler(csrfTokenRequestHandler)
				.ignoringRequestMatchers("/api/openapi/**", "/swagger-ui/**", "/v3/api-docs/**"))
				.cors(cors -> cors.configurationSource(corsConfigurationSource))
				.headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()))
				.httpBasic(AbstractHttpConfigurer::disable).formLogin(AbstractHttpConfigurer::disable)
				.authorizeHttpRequests(auth -> auth
						// OpenAPI/Swagger documentation - permit all
						.requestMatchers("/api/openapi/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/auth/csrf").permitAll()

						// All other requests are handled by @PreAuthorize annotations on controllers
						// Using permitAll here to allow method-level security annotations to take
						// effect
						.anyRequest().permitAll())
				.securityContext(
						securityContext -> securityContext.securityContextRepository(securityContextRepository()))
				.sessionManagement(session -> session.sessionAuthenticationStrategy(sessionAuthenticationStrategy()))
				.rememberMe(rememberMe -> rememberMe.rememberMeServices(rememberMeServices))
				.exceptionHandling(
						exceptionHandling -> exceptionHandling.authenticationEntryPoint(authenticationEntryPoint())
								.accessDeniedHandler(accessDeniedHandler()))
				.logout(logout -> logout.logoutUrl("/api/auth/logout").deleteCookies("LEARNHUBSESSION", "XSRF-TOKEN")
						.invalidateHttpSession(true).clearAuthentication(true)
						.logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.OK)));

		return http.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
		return config.getAuthenticationManager();
	}

	@Bean
	public SecurityContextRepository securityContextRepository() {
		return new HttpSessionSecurityContextRepository();
	}

	@Bean
	public SessionAuthenticationStrategy sessionAuthenticationStrategy() {
		return new ChangeSessionIdAuthenticationStrategy();
	}

	@Bean
	public AuthenticationEntryPoint authenticationEntryPoint() {
		return new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED);
	}

	@Bean
	public AccessDeniedHandler accessDeniedHandler() {
		return (request, response, accessDeniedException) -> response.sendError(HttpStatus.FORBIDDEN.value(),
				accessDeniedException.getMessage());
	}
}
