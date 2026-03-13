// Removed ApiResponse wrapper - server now returns data directly

export interface ActivityDocument {
  id: string;
  filename: string;
  fileSize: number;
  type: string;
}

export interface ActivityMarkdown {
  id: string;
  type: string;
  content: string;
}

export interface BreakAfter {
  type: "break";
  id: string;
  duration: number;
  description: string;
  reasons: string[];
}

export interface Activity {
  id: string;
  name: string;
  description: string;
  source?: string;
  ageMin: number;
  ageMax: number;
  format: string;
  resourcesNeeded: string[];
  bloomLevel: string;
  durationMinMinutes: number;
  durationMaxMinutes?: number;
  prepTimeMinutes?: number;
  cleanupTimeMinutes?: number;
  mentalLoad?: string;
  physicalEnergy?: string;
  topics: string[];
  documents: ActivityDocument[];
  markdowns: ActivityMarkdown[];
  createdAt?: string;
  type: "activity";
  // Break that should happen after this activity
  breakAfter?: BreakAfter;
}

// Individual recommendation containing activities and scoring breakdown
export interface Recommendation {
  activities: Activity[]; // Activities in this recommendation
  score: number; // Relevance score for this recommendation (0-100)
  scoreBreakdown: Record<string, CategoryScore>; // Detailed scoring breakdown by category
}

// Category score details for recommendation scoring
export interface CategoryScore {
  category: string;
  score: number;
  impact: number;
  priorityMultiplier: number;
  isPriority: boolean;
}

// Updated to match server API - now returns individual recommendations
export interface ResultsData {
  activities: Recommendation[]; // List of individual recommendations (matches server field name)
  total: number; // Total number of recommendations
  searchCriteria: Record<string, string>; // Search criteria used for the request
  generatedAt: string; // ISO timestamp when recommendations were generated
}

// Legacy Break interface removed - breaks are now embedded in activities via breakAfter field

export interface FilterOptions {
  format: string[];
  resourcesAvailable: string[];
  bloomLevel: string[];
  topics: string[];
  mentalLoad: string[];
  physicalEnergy: string[];
}

export interface ActivitiesResponse {
  activities: Activity[];
  total: number; // Matches server field name
  limit: number;
  offset: number;
}

// User response types
export interface User {
  id: number;
  email: string;
  firstName?: string;
  lastName?: string;
  name?: string;
  role: string;
  isVerified?: boolean;
  createdAt?: string;
}

// Search history types
export interface SearchHistoryEntry {
  id: number;
  searchCriteria: Record<string, string | number | boolean | string[]>;
  createdAt: string;
}

export interface SearchHistoryResponse {
  searchHistory: SearchHistoryEntry[];
  pagination: {
    limit: number;
    offset: number;
    count: number;
  };
}

// Field values types - matches server FieldValuesResponse
export interface FieldValues {
  format: string[];
  resourcesAvailable: string[];
  bloomLevel: string[];
  topics: string[];
  priorityCategories: string[];
  mentalLoad: string[];
  physicalEnergy: string[];
}

// Lesson plan data type
export interface LessonPlanData {
  activities: Activity[];
  totalDurationMinutes: number;
  breaks?: Array<{
    description: string;
    duration: number;
    reasons: string[];
  }>;
  ordering_strategy?: string;
  createdAt?: string;
  title?: string;
  searchCriteria?: Record<string, string | number | boolean | string[]>;
}
