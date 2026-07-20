package com.learnhub.config;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {

	@Value("${app.client.allowed-origins:}")
	private String allowedOrigins;

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		List<String> configuredOrigins = Stream.of(allowedOrigins.split(",")).map(String::trim)
				.filter(origin -> !origin.isEmpty()).toList();
		if (!configuredOrigins.isEmpty()) {
			configuration.setAllowedOrigins(configuredOrigins);
		} else {
			// No specific origins configured - allow all via pattern (required for
			// allowCredentials=true; setAllowedOrigins("*") does not work with credentials)
			configuration.setAllowedOriginPatterns(Arrays.asList("*"));
		}
		configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(Arrays.asList("*"));
		configuration.setExposedHeaders(Arrays.asList("Content-Type"));
		configuration.setAllowCredentials(true);
		configuration.setMaxAge(3600L);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}
}
