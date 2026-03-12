export interface FormData {
  targetAge: number;
  format: string[];
  resourcesNeeded: string[];
  bloomLevels: string[];
  targetDuration: number;
  topics: string[];
  allowLessonPlans: boolean;
  maxActivityCount: number;
  includeBreaks: boolean;
  priorityCategories: string[]; // Categories to prioritize in scoring
}

/**
 * Convert search criteria from search history back to form data format
 */
export function convertSearchCriteriaToFormData(
  searchCriteria: Record<string, unknown>,
): Partial<FormData> {
  const formData: Partial<FormData> = {};

  // Convert basic fields
  if (searchCriteria.targetAge !== undefined) {
    formData.targetAge =
      typeof searchCriteria.targetAge === "number"
        ? searchCriteria.targetAge
        : parseInt(String(searchCriteria.targetAge), 10);
  }

  if (searchCriteria.targetDuration !== undefined) {
    formData.targetDuration =
      typeof searchCriteria.targetDuration === "number"
        ? searchCriteria.targetDuration
        : parseInt(String(searchCriteria.targetDuration), 10);
  }

  if (searchCriteria.allowLessonPlans !== undefined) {
    formData.allowLessonPlans =
      typeof searchCriteria.allowLessonPlans === "boolean"
        ? searchCriteria.allowLessonPlans
        : String(searchCriteria.allowLessonPlans).toLowerCase() === "true";
  }

  if (searchCriteria.maxActivityCount !== undefined) {
    formData.maxActivityCount =
      typeof searchCriteria.maxActivityCount === "number"
        ? searchCriteria.maxActivityCount
        : parseInt(String(searchCriteria.maxActivityCount), 10);
  }

  if (searchCriteria.includeBreaks !== undefined) {
    formData.includeBreaks =
      typeof searchCriteria.includeBreaks === "boolean"
        ? searchCriteria.includeBreaks
        : String(searchCriteria.includeBreaks).toLowerCase() === "true";
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
  if (searchCriteria.availableResources) {
    if (Array.isArray(searchCriteria.availableResources)) {
      formData.resourcesNeeded = searchCriteria.availableResources.map(String);
    } else if (typeof searchCriteria.availableResources === "string") {
      // Handle comma-separated values from URL parameters
      formData.resourcesNeeded = searchCriteria.availableResources
        .split(",")
        .map((s) => s.trim())
        .filter((s) => s.length > 0);
    }
  } else if (searchCriteria.resourcesNeeded) {
    if (Array.isArray(searchCriteria.resourcesNeeded)) {
      formData.resourcesNeeded = searchCriteria.resourcesNeeded.map(String);
    } else if (typeof searchCriteria.resourcesNeeded === "string") {
      formData.resourcesNeeded = [searchCriteria.resourcesNeeded];
    }
  }

  if (searchCriteria.bloomLevels) {
    if (Array.isArray(searchCriteria.bloomLevels)) {
      formData.bloomLevels = searchCriteria.bloomLevels.map(String);
    } else if (typeof searchCriteria.bloomLevels === "string") {
      // Handle comma-separated values from URL parameters
      formData.bloomLevels = searchCriteria.bloomLevels
        .split(",")
        .map((s) => s.trim())
        .filter((s) => s.length > 0);
    }
  }

  if (searchCriteria.preferredTopics) {
    if (Array.isArray(searchCriteria.preferredTopics)) {
      formData.topics = searchCriteria.preferredTopics.map(String);
    } else if (typeof searchCriteria.preferredTopics === "string") {
      // Handle comma-separated values from URL parameters
      formData.topics = searchCriteria.preferredTopics
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
  if (searchCriteria.priorityCategories) {
    if (Array.isArray(searchCriteria.priorityCategories)) {
      formData.priorityCategories =
        searchCriteria.priorityCategories.map(String);
    } else if (typeof searchCriteria.priorityCategories === "string") {
      // Handle comma-separated values from URL parameters
      formData.priorityCategories = searchCriteria.priorityCategories
        .split(",")
        .map((s) => s.trim())
        .filter((s) => s.length > 0);
    }
  } else if (searchCriteria.priority_fields) {
    // Handle legacy priority_fields for backward compatibility
    if (Array.isArray(searchCriteria.priority_fields)) {
      formData.priorityCategories = searchCriteria.priority_fields.map(String);
    } else if (typeof searchCriteria.priority_fields === "string") {
      formData.priorityCategories = [searchCriteria.priority_fields];
    }
  } else {
    // Default to empty array if no priority categories
    formData.priorityCategories = [];
  }

  return formData;
}
