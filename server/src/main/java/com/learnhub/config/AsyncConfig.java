package com.learnhub.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

@Configuration
public class AsyncConfig {

	@Bean(destroyMethod = "shutdown")
	ExecutorService markdownGenerationExecutor(
			@Value("${app.markdown-generation.max-concurrency:4}") int maxConcurrency) {
		int poolSize = Math.max(1, maxConcurrency);
		return Executors.newFixedThreadPool(poolSize, new CustomizableThreadFactory("markdown-generation-"));
	}
}
