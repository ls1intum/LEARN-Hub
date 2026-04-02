package com.learnhub.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SanitizationServiceTest {

	private final SanitizationService sanitizationService = new SanitizationService();

	@Test
	void sanitizeReplacesTypicalAiTypographyWithStandardEquivalents() {
		String sanitized = sanitizationService
				.sanitize("\u2014 \u2013 \u2026 \u201Ctext\u201D \u2018quote\u2019 \u00A0a\u200Bb\u200Dc\u200Cd");

		assertThat(sanitized).isEqualTo("- - ... \"text\" 'quote'  abcd");
	}

	@Test
	void sanitizeReplacesAdditionalDashSpaceAndEntityVariants() {
		String sanitized = sanitizationService
				.sanitize("a\u2010b\u2011c\u2012d\u2015e\u2212f\u00ADg\u202Fh\u2060i &ndash; &#8211; &#x2013; &mdash; &#8212; &#x2014;");

		assertThat(sanitized).isEqualTo("a-b-c-d-e-fg hi - - - - - -");
	}

	@Test
	void sanitizeReturnsEmptyStringForNull() {
		assertThat(sanitizationService.sanitize(null)).isEmpty();
	}
}
