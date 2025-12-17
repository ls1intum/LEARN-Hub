package com.learnhub.config;

import com.learnhub.security.JwtRequestFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private JwtRequestFilter jwtRequestFilter;

    @Autowired
    private CorsConfigurationSource corsConfigurationSource;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints - OpenAPI/Swagger
                .requestMatchers("/api/hello", "/hello", "/api/openapi/**", "/openapi.json", 
                                 "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                
                // Public auth endpoints (match Flask exactly)
                .requestMatchers("/api/auth/register-teacher", "/api/auth/login", "/api/auth/verify",
                                 "/api/auth/verification-code", "/api/auth/refresh", "/api/auth/admin/login",
                                 "/api/auth/reset-password").permitAll()
                
                // Auth-required endpoints
                .requestMatchers("/api/auth/me", "/api/auth/logout").authenticated()
                
                // Admin-only auth endpoints
                .requestMatchers("/api/auth/users", "/api/auth/users/**").hasRole("ADMIN")
                
                // Public activity endpoints (GET only)
                .requestMatchers(HttpMethod.GET, "/api/activities/", "/api/activities/*", 
                                "/api/activities/*/pdf", "/api/activities/recommendations").permitAll()
                
                // Admin-only activity endpoints
                .requestMatchers(HttpMethod.POST, "/api/activities/", "/api/activities/upload-and-create").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/activities/*").hasRole("ADMIN")
                
                // Public activity endpoints (POST for lesson-plan is public)
                .requestMatchers("/api/activities/lesson-plan").permitAll()
                
                // Public meta endpoints
                .requestMatchers("/api/meta/**").permitAll()
                
                // Document endpoints - admin only for upload, public for download
                .requestMatchers(HttpMethod.POST, "/api/documents/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/documents/*", "/api/documents/*/info").permitAll()
                
                // History endpoints - authenticated users only
                .requestMatchers("/api/history/**").authenticated()
                
                // Everything else requires authentication
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

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
}
