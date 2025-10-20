// API-specific type definitions to replace any/unknown types

// Generic API response wrapper (when server returns wrapped responses)
export interface ApiResponse<T = unknown> {
  success: boolean;
  data?: T;
  message?: string;
  error?: string;
}

// Error response structure
export interface ApiError {
  error: string;
  message?: string;
  details?: Record<string, string>;
}

// Generic request body for API calls
export interface ApiRequestBody {
  [key: string]:
    | string
    | number
    | boolean
    | string[]
    | number[]
    | null
    | undefined;
}

// Form data for dynamic forms
export interface FormFieldData {
  [key: string]: string | number | boolean | string[] | null | undefined;
}

// PDF processing response
export interface PdfProcessingResponse {
  document_id: number;
  extracted_data: FormFieldData;
  confidence: number;
  text_length: number;
  extraction_quality: string;
}

// Upload and create activity response
export interface UploadCreateResponse {
  activity: FormFieldData;
  document_id: number;
  extraction_confidence: number;
  extraction_quality: string;
}

// Activity creation request
export interface CreateActivityRequest {
  name: string;
  description: string;
  source?: string;
  age_min: number;
  age_max: number;
  format: string;
  resources_needed: string[];
  bloom_level: string;
  duration_min_minutes: number;
  duration_max_minutes?: number;
  prep_time_minutes?: number;
  cleanup_time_minutes?: number;
  mental_load?: string;
  physical_energy?: string;
  topics: string[];
  document_id?: number;
}

// User creation/update request
export interface UserRequest {
  email: string;
  role: "TEACHER" | "ADMIN";
  password?: string;
}

// Favorite activity request
export interface FavoriteActivityRequest {
  activity_id: number;
  name?: string;
}

// Favorite lesson plan request
export interface FavoriteLessonPlanRequest {
  activity_ids: number[];
  name?: string;
  lesson_plan: import("./activity").LessonPlanData;
}

// Lesson plan generation request
export interface LessonPlanRequest {
  activities: Array<{
    id: number;
    name: string;
    description: string;
    source?: string;
    age_min: number;
    age_max: number;
    format: string;
    resources_needed: string[];
    bloom_level: string;
    duration_min_minutes: number;
    duration_max_minutes?: number;
    prep_time_minutes?: number;
    cleanup_time_minutes?: number;
    mental_load?: string;
    physical_energy?: string;
    topics: string[];
    document_id?: number;
    created_at?: string;
    type: "activity";
  }>;
  search_criteria: FormFieldData;
  breaks?: Array<{
    position: number;
    duration: number;
    description: string;
    reasons: string[];
  }>;
  total_duration?: number;
}

// Search criteria for recommendations
export interface SearchCriteria {
  name?: string;
  age_min?: number;
  age_max?: number;
  format?: string[];
  resources_available?: string[];
  resources_needed?: string[]; // allow client alias used in LibraryPage
  bloom_level?: string[];
  topics?: string[];
  duration_min?: number;
  duration_max?: number;
  mental_load?: string[];
  physical_energy?: string[];
  priority_categories?: string[];
  limit?: number;
  offset?: number;
}

// Activity favorites response
export interface ActivityFavoritesResponse {
  favourites: Array<{
    id: number;
    favourite_type: string;
    activity_id: number;
    name: string | null;
    created_at: string;
  }>;
  pagination: {
    limit: number;
    offset: number;
    count: number;
  };
}

// Lesson plan favorites response
export interface LessonPlanFavoritesResponse {
  favourites: Array<{
    id: number;
    favourite_type: string;
    name: string | null;
    activity_ids: number[];
    lesson_plan?: import("./activity").LessonPlanData;
    created_at: string;
  }>;
  pagination: {
    limit: number;
    offset: number;
    count: number;
  };
}

// Favorite status response
export interface FavoriteStatusResponse {
  is_favourited: boolean;
}

// Users response
export interface UsersResponse {
  users: Array<{
    id: number;
    email: string;
    name?: string;
    role: string;
    is_verified?: boolean;
    created_at?: string;
  }>;
}

// Scoring insights response
export interface ScoringInsightsResponse {
  insights: Array<{
    category: string;
    description: string;
    impact: number;
  }>;
}
