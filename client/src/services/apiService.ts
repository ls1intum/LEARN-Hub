import { authService } from "./authService";
import type {
  Activity,
  ActivitiesResponse,
  ResultsData,
  SearchHistoryResponse,
  User,
  LessonPlanInfo,
  Document,
  FieldValues,
} from "@/types/activity";
import type {
  PdfProcessingResponse,
  UploadCreateResponse,
  CreateActivityRequest,
  UserRequest,
  FavoriteActivityRequest,
  FavoriteLessonPlanRequest,
  LessonPlanRequest,
  SearchCriteria,
  ActivityFavoritesResponse,
  LessonPlanFavoritesResponse,
  FavoriteStatusResponse,
  UsersResponse,
  ScoringInsightsResponse,
} from "@/types/api";

// Legacy interfaces removed - now using types from activity.ts

/**
 * Standardized API response handler
 * All API calls should use this instead of direct authService calls
 */
export class ApiService {
  /**
   * Make authenticated API request with standardized response handling
   * Server now returns data directly without ApiResponse wrapper
   */
  static async request<T = Record<string, unknown>>(
    url: string,
    options: RequestInit = {},
  ): Promise<T> {
    const response = await authService.makeAuthenticatedRequest(url, options);

    if (!response || !response.ok) {
      const errorData = response
        ? await response.json().catch(() => ({}) as Record<string, string>)
        : {};

      // Handle Pydantic validation errors (422)
      if (response?.status === 422 && errorData.detail) {
        const details = errorData.detail;
        if (Array.isArray(details)) {
          const fieldErrors = details
            .map((err: { loc?: (string | number)[]; msg?: string }) => {
              const fieldName = err.loc?.[1] || "field";
              const message = err.msg || "Invalid input";
              return `${fieldName}: ${message}`;
            })
            .join("; ");
          throw new Error(fieldErrors);
        }
      }

      throw new Error(
        (errorData.error as string) ||
          `HTTP error! status: ${response?.status || "unknown"}`,
      );
    }

    const responseData = await response.json();

    // Server now returns data directly without wrapper
    return responseData as T;
  }

  /**
   * Get activities with pagination
   */
  static async getActivities(params: SearchCriteria = {}) {
    const queryParams = new URLSearchParams();
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== "") {
        if (Array.isArray(value)) {
          value.forEach((v) => queryParams.append(key, String(v)));
        } else {
          queryParams.append(key, String(value));
        }
      }
    });

    return this.request<ActivitiesResponse>(
      `/api/activities/?${queryParams.toString()}`,
    );
  }

  /**
   * Get activity by ID
   */
  static async getActivity(id: number) {
    return this.request<Activity>(`/api/activities/${id}`);
  }

  /**
   * Get multiple activities by IDs
   */
  static async getActivitiesByIds(ids: number[]) {
    const promises = ids.map((id) => this.getActivity(id));
    const results = await Promise.all(promises);
    return results;
  }

  /**
   * Get recommendations
   */
  static async getRecommendations(params: string): Promise<ResultsData> {
    return this.request<ResultsData>(
      `/api/activities/recommendations?${params}`,
    );
  }

  /**
   * Get search history
   */
  static async getSearchHistory(limit = 50, offset = 0) {
    return this.request<SearchHistoryResponse>(
      `/api/history/search?limit=${limit}&offset=${offset}`,
    );
  }

  /**
   * Delete search history entry
   */
  static async deleteSearchHistoryEntry(historyId: number) {
    return this.request(`/api/history/search/${historyId}`, {
      method: "DELETE",
    });
  }

  /**
   * Get activity favourites
   */
  static async getActivityFavourites(limit = 50, offset = 0) {
    return this.request<ActivityFavoritesResponse>(
      `/api/history/favourites/activities?limit=${limit}&offset=${offset}`,
    );
  }

  /**
   * Get lesson plan favourites
   */
  static async getLessonPlanFavourites(limit = 50, offset = 0) {
    return this.request<LessonPlanFavoritesResponse>(
      `/api/history/favourites/lesson-plans?limit=${limit}&offset=${offset}`,
    );
  }

  /**
   * Save activity favourite
   */
  static async saveActivityFavourite(data: FavoriteActivityRequest) {
    return this.request("/api/history/favourites/activities", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(data),
    });
  }

  /**
   * Save lesson plan favourite
   */
  static async saveLessonPlanFavourite(data: FavoriteLessonPlanRequest) {
    return this.request("/api/history/favourites/lesson-plans", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(data),
    });
  }

  /**
   * Remove activity favourite
   */
  static async removeActivityFavourite(activityId: number) {
    return this.request(`/api/history/favourites/activities/${activityId}`, {
      method: "DELETE",
    });
  }

  /**
   * Delete favourite (by favourite ID)
   */
  static async deleteFavourite(favouriteId: number) {
    return this.request(`/api/history/favourites/${favouriteId}`, {
      method: "DELETE",
    });
  }

  /**
   * Check if activity is favourited
   */
  static async checkActivityFavouriteStatus(activityId: number) {
    return this.request<FavoriteStatusResponse>(
      `/api/history/favourites/activities/${activityId}/status`,
    );
  }

  // Legacy methods removed

  /**
   * Get current user
   */
  static async getCurrentUser() {
    return this.request<User>("/api/auth/me");
  }

  /**
   * Get users (admin only)
   */
  static async getUsers() {
    return this.request<UsersResponse>("/api/auth/users");
  }

  /**
   * Create user (admin only)
   */
  static async createUser(data: UserRequest) {
    return this.request("/api/auth/users", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(data),
    });
  }

  /**
   * Update user (admin only)
   */
  static async updateUser(userId: number, data: UserRequest) {
    return this.request(`/api/auth/users/${userId}`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(data),
    });
  }

  /**
   * Delete user (admin only)
   */
  static async deleteUser(userId: number) {
    return this.request(`/api/auth/users/${userId}`, {
      method: "DELETE",
    });
  }

  /**
   * Update current user's profile
   */
  static async updateProfile(data: import("@/types/api").UpdateProfileRequest) {
    return this.request("/api/auth/me", {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(data),
    });
  }

  /**
   * Delete current user's account
   */
  static async deleteProfile() {
    return this.request("/api/auth/me", {
      method: "DELETE",
    });
  }

  /**
   * Upload PDF
   */
  static async uploadPdf(file: File) {
    const formData = new FormData();
    formData.append("pdf_file", file);

    return this.request("/api/documents/upload_pdf", {
      method: "POST",
      body: formData,
    });
  }

  /**
   * Get PDF info
   */
  static async getPdfInfo(documentId: number) {
    return this.request(`/api/documents/${documentId}/info`);
  }

  /**
   * Download PDF
   */
  static async downloadPdf(documentId: number) {
    const response = await authService.makeAuthenticatedRequest(
      `/api/documents/${documentId}`,
    );

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    return response.blob();
  }

  /**
   * Create activity
   */
  static async createActivity(data: CreateActivityRequest) {
    return this.request("/api/activities/create", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(data),
    });
  }

  /**
   * Delete activity (admin only)
   */
  static async deleteActivity(activityId: number) {
    return this.request(`/api/activities/${activityId}`, {
      method: "DELETE",
    });
  }

  /**
   * Generate lesson plan PDF
   */
  static async generateLessonPlan(data: LessonPlanRequest) {
    const response = await authService.makeAuthenticatedRequest(
      "/api/activities/lesson-plan",
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(data),
      },
    );

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(
        errorData.message || `HTTP error! status: ${response.status}`,
      );
    }

    return response.blob();
  }

  /**
   * Get lesson plan info
   */
  static async getLessonPlanInfo(activities: Activity[]) {
    return this.request<LessonPlanInfo>("/api/activities/lesson-plan/info", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ activities }),
    });
  }

  /**
   * Get PDF by activity ID
   */
  static async getActivityPdf(activityId: number) {
    const response = await authService.makeAuthenticatedRequest(
      `/api/activities/${activityId}/pdf`,
    );

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    return response.blob();
  }

  /**
   * Get PDF by document ID
   */
  static async getDocumentPdf(documentId: number) {
    const response = await authService.makeAuthenticatedRequest(
      `/api/documents/${documentId}`,
    );

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    return response.blob();
  }

  /**
   * Get PDF document info by document ID
   */
  static async getDocumentInfo(documentId: number) {
    return this.request<Document>(`/api/documents/${documentId}/info`);
  }

  /**
   * Process PDF document and extract activity data
   */
  static async processPdf(documentId: number) {
    return this.request<PdfProcessingResponse>(
      `/api/documents/${documentId}/process`,
      {
        method: "POST",
      },
    );
  }

  /**
   * Upload PDF and create activity in one step
   */
  static async uploadAndCreateActivity(file: File) {
    const formData = new FormData();
    formData.append("pdf_file", file);

    return this.request<UploadCreateResponse>(
      "/api/activities/upload-and-create",
      {
        method: "POST",
        body: formData,
      },
    );
  }

  /**
   * Get field values from server
   */
  static async getFieldValues() {
    return this.request<FieldValues>("/api/meta/field-values");
  }

  /**
   * Get current environment from server
   */
  static async getEnvironment() {
    return this.request<{ environment: string }>("/api/meta/environment");
  }

  /**
   * Get scoring insights
   */
  static async getScoringInsights() {
    return this.request<ScoringInsightsResponse>(
      "/api/activities/scoring-insights",
    );
  }
}

// Export singleton instance for backward compatibility
export const apiService = ApiService;
