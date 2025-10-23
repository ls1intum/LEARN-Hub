// Removed ApiResponse wrapper - server now returns data directly

export interface BreakAfter {
  type: "break";
  id: string;
  duration: number;
  description: string;
  reasons: string[];
}

export interface Activity {
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
  // Break that should happen after this activity
  break_after?: BreakAfter;
}

// Individual recommendation containing activities and scoring breakdown
export interface Recommendation {
  activities: Activity[]; // Activities in this recommendation
  score: number; // Relevance score for this recommendation (0-100)
  score_breakdown: Record<string, CategoryScore>; // Detailed scoring breakdown by category
}

// Category score details for recommendation scoring
export interface CategoryScore {
  category: string;
  score: number;
  impact: number;
  priority_multiplier: number;
  is_priority: boolean;
}

// Updated to match server API - now returns individual recommendations
export interface ResultsData {
  activities: Recommendation[]; // List of individual recommendations (matches server field name)
  total: number; // Total number of recommendations
  search_criteria: Record<string, string>; // Search criteria used for the request
  generated_at: string; // ISO timestamp when recommendations were generated
}

// Legacy Break interface removed - breaks are now embedded in activities via break_after field

export interface FilterOptions {
  format: string[];
  resources_available: string[];
  bloom_level: string[];
  topics: string[];
}

export interface ActivitiesResponse {
  activities: Activity[];
  total: number; // Matches server field name
  limit: number;
  offset: number;
}

export interface FavoriteActivity {
  id: string;
  name: string;
  source: string;
  age_min: number;
  age_max: number;
  format: string;
  duration_min_minutes: number;
  duration_max_minutes?: number;
  topics: string[];
  favorited_at: string;
  backendData?: Record<string, string | number | boolean | string[]>;
}

// User response types
export interface User {
  id: number;
  email: string;
  first_name?: string;
  last_name?: string;
  name?: string;
  role: string;
  is_verified?: boolean;
  created_at?: string;
}

export interface UserLoginData {
  user: User;
  access_token: string;
  refresh_token: string;
  expires_in?: number;
}

// Search history types
export interface SearchHistoryEntry {
  id: number;
  search_criteria: Record<string, string | number | boolean | string[]>;
  created_at: string;
}

export interface SearchHistoryResponse {
  search_history: SearchHistoryEntry[];
  pagination: {
    limit: number;
    offset: number;
    count: number;
  };
}

// Favorites types
export interface Favorite {
  id: number;
  name: string;
  activities: Activity[];
  search_criteria: Record<string, string | number | boolean | string[]>;
  total_duration: number;
  created_at: string;
}

export interface FavoritesResponse {
  favorites: Favorite[];
  pagination: {
    limit: number;
    offset: number;
    count: number;
  };
}

// Lesson plan types
export interface LessonPlanInfo {
  title: string;
  total_duration: number;
  activity_count: number;
  topics_covered: string[];
  bloom_levels: string[];
  age_range: string;
  formats: string[];
}

// Document types
export interface Document {
  id: number;
  filename: string;
  file_size: number;
  mime_type?: string;
  created_at: string;
}

// Field values types - matches server FieldValuesResponse
export interface FieldValues {
  format: string[];
  resources_available: string[];
  bloom_level: string[];
  topics: string[];
  priority_categories: string[];
  mental_load: string[];
  physical_energy: string[];
}

// Lesson plan data type
export interface LessonPlanData {
  activities: Activity[];
  total_duration_minutes: number;
  breaks?: Array<{
    description: string;
    duration: number;
    reasons: string[];
  }>;
  ordering_strategy?: string;
  created_at?: string;
  title?: string;
  search_criteria?: Record<string, string | number | boolean | string[]>;
}
