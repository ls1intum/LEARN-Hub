// API-specific type definitions to replace any/unknown types

// Form data for dynamic forms
export interface FormFieldData {
  [key: string]: string | number | boolean | string[] | null | undefined;
}

// Upload PDF draft response (2-step flow)
export interface UploadPdfDraftResponse {
  documentId: string;
  extractedData: FormFieldData;
  extractionConfidence: number;
  extractionQuality: string;
}

export interface UploadPdfDraftOptions {
  extractMetadata?: boolean;
}

// Activity markdowns generation response (all three types)
export interface ActivityMarkdownsResponse {
  documentId: string;
  deckblattMarkdown?: string;
  artikulationsschemaMarkdown?: string;
  hintergrundwissenMarkdown?: string;
  uebungMarkdown?: string;
  uebungLoesungMarkdown?: string;
}

// Activity creation request
export interface CreateActivityRequest {
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
  documentId?: number | string;
  artikulationsschemaMarkdown?: string;
  deckblattMarkdown?: string;
  hintergrundwissenMarkdown?: string;
  uebungMarkdown?: string;
  uebungLoesungMarkdown?: string;
}

// Activity update request
export interface UpdateActivityRequest {
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
  artikulationsschemaMarkdown?: string;
  deckblattMarkdown?: string;
  hintergrundwissenMarkdown?: string;
  uebungMarkdown?: string;
  uebungLoesungMarkdown?: string;
}

// User creation/update request
export interface UserRequest {
  email: string;
  firstName: string;
  lastName: string;
  role: "TEACHER" | "ADMIN";
  password?: string;
}

// Profile update request for self-service account management
export interface UpdateProfileRequest {
  email?: string;
  firstName?: string;
  lastName?: string;
  password?: string;
}

// Favorite activity request
export interface FavoriteActivityRequest {
  activityId: string;
  name?: string;
}

// Favorite lesson plan request
export interface FavoriteLessonPlanRequest {
  activityIds: string[];
  name?: string;
  lessonPlan: import("./activity").LessonPlanData;
}

// Lesson plan generation request
export interface LessonPlanRequest {
  activities: Array<{
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
    documents?: Array<{
      id: string;
      filename: string;
      fileSize: number;
      type: string;
    }>;
    markdowns?: Array<{ id: string; type: string; content?: string }>;
    createdAt?: string;
    type: "activity";
  }>;
  searchCriteria: FormFieldData;
  breaks?: Array<{
    position: number;
    duration: number;
    description: string;
    reasons: string[];
  }>;
  totalDuration?: number;
}

// Search criteria for recommendations
export interface SearchCriteria {
  name?: string;
  ageMin?: number;
  ageMax?: number;
  format?: string[];
  resourcesAvailable?: string[];
  resourcesNeeded?: string[]; // allow client alias used in LibraryPage
  bloomLevel?: string[];
  topics?: string[];
  durationMin?: number;
  durationMax?: number;
  mentalLoad?: string[];
  physicalEnergy?: string[];
  priorityCategories?: string[];
  limit?: number;
  offset?: number;
}

// Activity favorites response
export interface ActivityFavoritesResponse {
  favourites: Array<{
    id: string;
    favouriteType: string;
    activityId: string;
    name: string | null;
    createdAt: string;
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
    id: string;
    favouriteType: string;
    name: string | null;
    activityIds: string[];
    lessonPlan?: import("./activity").LessonPlanData;
    createdAt: string;
  }>;
  pagination: {
    limit: number;
    offset: number;
    count: number;
  };
}

// Favorite status response
export interface FavoriteStatusResponse {
  isFavourited: boolean;
}

// Users response
export interface UsersResponse {
  users: Array<{
    id: number;
    email: string;
    name?: string;
    role: string;
    isVerified?: boolean;
    createdAt?: string;
  }>;
}
