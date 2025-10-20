export interface FormData {
  target_age: number;
  format: string[];
  resources_needed: string[];
  bloom_levels: string[];
  target_duration: number;
  topics: string[];
  allow_lesson_plans: boolean;
  max_activity_count: number;
  include_breaks: boolean;
  priority_categories: string[]; // Categories to prioritize in scoring
}

/**
 * Convert search criteria from search history back to form data format
 */
export function convertSearchCriteriaToFormData(
  searchCriteria: Record<string, unknown>,
): Partial<FormData> {
  const formData: Partial<FormData> = {};

  // Convert basic fields
  if (searchCriteria.target_age !== undefined) {
    formData.target_age =
      typeof searchCriteria.target_age === "number"
        ? searchCriteria.target_age
        : parseInt(String(searchCriteria.target_age), 10);
  }

  if (searchCriteria.target_duration !== undefined) {
    formData.target_duration =
      typeof searchCriteria.target_duration === "number"
        ? searchCriteria.target_duration
        : parseInt(String(searchCriteria.target_duration), 10);
  }

  if (searchCriteria.allow_lesson_plans !== undefined) {
    formData.allow_lesson_plans =
      typeof searchCriteria.allow_lesson_plans === "boolean"
        ? searchCriteria.allow_lesson_plans
        : String(searchCriteria.allow_lesson_plans).toLowerCase() === "true";
  }

  if (searchCriteria.max_activity_count !== undefined) {
    formData.max_activity_count =
      typeof searchCriteria.max_activity_count === "number"
        ? searchCriteria.max_activity_count
        : parseInt(String(searchCriteria.max_activity_count), 10);
  }

  if (searchCriteria.include_breaks !== undefined) {
    formData.include_breaks =
      typeof searchCriteria.include_breaks === "boolean"
        ? searchCriteria.include_breaks
        : String(searchCriteria.include_breaks).toLowerCase() === "true";
  }

  // Convert array fields (handle both arrays and single strings)
  if (searchCriteria.format) {
    if (Array.isArray(searchCriteria.format)) {
      formData.format = searchCriteria.format.map(String);
    } else if (typeof searchCriteria.format === "string") {
      // Handle comma-separated values from URL parameters
      formData.format = searchCriteria.format
        .split(",")
        .map((s) => s.trim())
        .filter((s) => s.length > 0);
    }
  }

  // Handle both client and server parameter names
  if (searchCriteria.available_resources) {
    if (Array.isArray(searchCriteria.available_resources)) {
      formData.resources_needed =
        searchCriteria.available_resources.map(String);
    } else if (typeof searchCriteria.available_resources === "string") {
      // Handle comma-separated values from URL parameters
      formData.resources_needed = searchCriteria.available_resources
        .split(",")
        .map((s) => s.trim())
        .filter((s) => s.length > 0);
    }
  } else if (searchCriteria.resources_needed) {
    if (Array.isArray(searchCriteria.resources_needed)) {
      formData.resources_needed = searchCriteria.resources_needed.map(String);
    } else if (typeof searchCriteria.resources_needed === "string") {
      formData.resources_needed = [searchCriteria.resources_needed];
    }
  }

  if (searchCriteria.bloom_levels) {
    if (Array.isArray(searchCriteria.bloom_levels)) {
      formData.bloom_levels = searchCriteria.bloom_levels.map(String);
    } else if (typeof searchCriteria.bloom_levels === "string") {
      // Handle comma-separated values from URL parameters
      formData.bloom_levels = searchCriteria.bloom_levels
        .split(",")
        .map((s) => s.trim())
        .filter((s) => s.length > 0);
    }
  }

  if (searchCriteria.preferred_topics) {
    if (Array.isArray(searchCriteria.preferred_topics)) {
      formData.topics = searchCriteria.preferred_topics.map(String);
    } else if (typeof searchCriteria.preferred_topics === "string") {
      // Handle comma-separated values from URL parameters
      formData.topics = searchCriteria.preferred_topics
        .split(",")
        .map((s) => s.trim())
        .filter((s) => s.length > 0);
    }
  } else if (searchCriteria.topics) {
    if (Array.isArray(searchCriteria.topics)) {
      formData.topics = searchCriteria.topics.map(String);
    } else if (typeof searchCriteria.topics === "string") {
      formData.topics = [searchCriteria.topics];
    }
  }

  // Convert priority categories
  if (searchCriteria.priority_categories) {
    if (Array.isArray(searchCriteria.priority_categories)) {
      formData.priority_categories =
        searchCriteria.priority_categories.map(String);
    } else if (typeof searchCriteria.priority_categories === "string") {
      // Handle comma-separated values from URL parameters
      formData.priority_categories = searchCriteria.priority_categories
        .split(",")
        .map((s) => s.trim())
        .filter((s) => s.length > 0);
    }
  } else if (searchCriteria.priority_fields) {
    // Handle legacy priority_fields for backward compatibility
    if (Array.isArray(searchCriteria.priority_fields)) {
      formData.priority_categories = searchCriteria.priority_fields.map(String);
    } else if (typeof searchCriteria.priority_fields === "string") {
      formData.priority_categories = [searchCriteria.priority_fields];
    }
  } else {
    // Default to empty array if no priority categories
    formData.priority_categories = [];
  }

  return formData;
}
