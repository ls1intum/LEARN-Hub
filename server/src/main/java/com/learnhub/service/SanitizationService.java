package com.learnhub.service;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SanitizationService {

	public String sanitize(String value) {
		if (value == null) {
			return "";
		}

		if (value.isEmpty()) {
			return value;
		}

		return value.replace("&ndash;", "-").replace("&#8211;", "-").replace("&#x2013;", "-").replace("&mdash;", "-")
				.replace("&#8212;", "-").replace("&#x2014;", "-").replace("\u00AD", "").replace("\u00A0", " ")
				.replace("\u202F", " ").replace("\u200B", "").replace("\u200C", "").replace("\u200D", "")
				.replace("\u2060", "").replace("\u2010", "-").replace("\u2011", "-").replace("\u2012", "-")
				.replace("\u2013", "-").replace("\u2026", "...").replace("\u201C", "\"").replace("\u201D", "\"")
				.replace("\u2018", "'").replace("\u2019", "'").replace("\u2014", "-").replace("\u2015", "-")
				.replace("\u2212", "-");
	}

	public List<String> sanitizeList(List<String> values) {
		if (values == null) {
			return null;
		}

		return values.stream().map(this::sanitize).toList();
	}
}
