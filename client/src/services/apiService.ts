import { authService } from "./authService";
import { ActivityApi } from "./activityApiService";
import { HistoryApi } from "./historyApiService";
import { UserApi } from "./userApiService";

/**
 * Core request handler, exported for use by domain-specific API modules.
 */
export const ApiRequestMixin = {
  /**
   * Make authenticated API request with standardized response handling.
   * Server now returns data directly without ApiResponse wrapper.
   */
  async request<T = Record<string, unknown>>(
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
  },
};

/**
 * Unified API service that re-exports all domain-specific methods.
 * Maintains backward compatibility — all existing callers continue to work.
 */
export class ApiService {
  // Core request handler
  static request = ApiRequestMixin.request;

  // --- Activity API ---
  static getActivities = ActivityApi.getActivities;
  static getActivity = ActivityApi.getActivity;
  static getActivitiesByIds = ActivityApi.getActivitiesByIds;
  static getRecommendations = ActivityApi.getRecommendations;
  static createActivity = ActivityApi.createActivity;
  static deleteActivity = ActivityApi.deleteActivity;
  static updateActivity = ActivityApi.updateActivity;
  static generateLessonPlan = ActivityApi.generateLessonPlan;
  static getActivityPdf = ActivityApi.getActivityPdf;
  static getMarkdownPdf = ActivityApi.getMarkdownPdf;
  static getMarkdownDocx = ActivityApi.getMarkdownDocx;
  static getFieldValues = ActivityApi.getFieldValues;
  static getEnvironment = ActivityApi.getEnvironment;
  static uploadPdfDraft = ActivityApi.uploadPdfDraft;
  static regenerateMetadata = ActivityApi.regenerateMetadata;
  static generateArtikulationsschema = ActivityApi.generateArtikulationsschema;
  static generateActivityMarkdowns = ActivityApi.generateActivityMarkdowns;
  static downloadActivityPdf = ActivityApi.downloadActivityPdf;
  static downloadActivityDocx = ActivityApi.downloadActivityDocx;
  static previewMarkdownPdf = ActivityApi.previewMarkdownPdf;

  // --- History API ---
  static getSearchHistory = HistoryApi.getSearchHistory;
  static deleteSearchHistoryEntry = HistoryApi.deleteSearchHistoryEntry;
  static getActivityFavourites = HistoryApi.getActivityFavourites;
  static getLessonPlanFavourites = HistoryApi.getLessonPlanFavourites;
  static saveActivityFavourite = HistoryApi.saveActivityFavourite;
  static saveLessonPlanFavourite = HistoryApi.saveLessonPlanFavourite;
  static removeActivityFavourite = HistoryApi.removeActivityFavourite;
  static deleteFavourite = HistoryApi.deleteFavourite;
  static checkActivityFavouriteStatus = HistoryApi.checkActivityFavouriteStatus;

  // --- User API ---
  static getCurrentUser = UserApi.getCurrentUser;
  static getUsers = UserApi.getUsers;
  static createUser = UserApi.createUser;
  static updateUser = UserApi.updateUser;
  static deleteUser = UserApi.deleteUser;
  static updateProfile = UserApi.updateProfile;
  static deleteProfile = UserApi.deleteProfile;
}

// Export singleton instance for backward compatibility
export const apiService = ApiService;
