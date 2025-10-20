/**
 * Field values for activity forms and filters.
 *
 * IMPORTANT: These values must be kept in sync with the server-side constants
 * defined in server/app/core/constants.py and server/app/core/recommendation/models.py
 *
 * When updating these values, ensure both client and server are updated simultaneously.
 */

const FIELD_VALUES = {
  format: ["unplugged", "digital", "hybrid"],
  resources_available: [
    "computers",
    "tablets",
    "handouts",
    "blocks",
    "electronics",
    "stationery",
  ],
  bloom_level: [
    "remember",
    "understand",
    "apply",
    "analyze",
    "evaluate",
    "create",
  ],
  topics: ["decomposition", "patterns", "abstraction", "algorithms"],
  mental_load: ["low", "medium", "high"],
  physical_energy: ["low", "medium", "high"],
  priority_categories: [
    "age_appropriateness",
    "bloom_level_match",
    "topic_relevance",
    "duration_fit",
  ],
  // Age range constants - must be kept in sync with server
  age_range: { min: 6, max: 15 },
  // Bloom's taxonomy order for scoring and progression - must match server BLOOM_ORDER
  bloom_order: [
    "Remember",
    "Understand",
    "Apply",
    "Analyze",
    "Evaluate",
    "Create",
  ],
  // Age filter tolerance - must match server AGE_FILTER_TOLERANCE
  age_filter_tolerance: 2,
  // Default pipeline limits - must match server constants
  default_max_activity_count: 2,
  default_recommendation_limit: 10,
};

// Legacy compatibility - these match the old field values structure
export const fieldValues = {
  format: FIELD_VALUES.format,
  resources_available: FIELD_VALUES.resources_available,
  bloom_level: FIELD_VALUES.bloom_level,
  topics: FIELD_VALUES.topics,
  mental_load: FIELD_VALUES.mental_load,
  physical_energy: FIELD_VALUES.physical_energy,
  priority_categories: FIELD_VALUES.priority_categories,
  age_range: FIELD_VALUES.age_range,
  bloom_order: FIELD_VALUES.bloom_order,
  age_filter_tolerance: FIELD_VALUES.age_filter_tolerance,
  default_max_activity_count: FIELD_VALUES.default_max_activity_count,
  default_recommendation_limit: FIELD_VALUES.default_recommendation_limit,
  // Add bloom_levels for backward compatibility
  bloom_levels: FIELD_VALUES.bloom_level,
};
