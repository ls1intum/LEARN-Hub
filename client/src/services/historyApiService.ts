import type {
  FavoriteActivityRequest,
  FavoriteLessonPlanRequest,
  ActivityFavoritesResponse,
  LessonPlanFavoritesResponse,
} from "@/types/api";
import type { SearchHistoryResponse } from "@/types/activity";
import { ApiRequestMixin } from "./apiService";

/**
 * History-related API methods: search history and favourites management.
 */
export const HistoryApi = {
  /**
   * Get search history
   */
  async getSearchHistory(limit = 50, offset = 0) {
    return ApiRequestMixin.request<SearchHistoryResponse>(
      `/api/history/search?limit=${limit}&offset=${offset}`,
    );
  },

  /**
   * Delete search history entry
   */
  async deleteSearchHistoryEntry(historyId: number) {
    return ApiRequestMixin.request(`/api/history/search/${historyId}`, {
      method: "DELETE",
    });
  },

  /**
   * Get activity favourites with full activity details and server-side pagination
   */
  async getActivityFavourites(
    params: {
      limit?: number;
      offset?: number;
      name?: string;
      ageMin?: number;
      ageMax?: number;
      durationMin?: number;
      durationMax?: number;
      format?: string[];
      bloomLevel?: string[];
      mentalLoad?: string[];
      physicalEnergy?: string[];
      resourcesNeeded?: string[];
      topics?: string[];
    } = {},
  ) {
    const { limit = 20, offset = 0, ...filters } = params;
    const query = new URLSearchParams({
      limit: String(limit),
      offset: String(offset),
    });
    Object.entries(filters).forEach(([key, value]) => {
      if (value === undefined || value === null || value === "") return;
      if (Array.isArray(value)) {
        value.forEach((entry) => query.append(key, String(entry)));
        return;
      }
      query.set(key, String(value));
    });
    return ApiRequestMixin.request<ActivityFavoritesResponse>(
      `/api/history/favourites/activities?${query}`,
    );
  },

  /**
   * Get lesson plan favourites
   */
  async getLessonPlanFavourites(limit = 50, offset = 0) {
    return ApiRequestMixin.request<LessonPlanFavoritesResponse>(
      `/api/history/favourites/lesson-plans?limit=${limit}&offset=${offset}`,
    );
  },

  /**
   * Save activity favourite
   */
  async saveActivityFavourite(data: FavoriteActivityRequest) {
    return ApiRequestMixin.request("/api/history/favourites/activities", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(data),
    });
  },

  /**
   * Save lesson plan favourite
   */
  async saveLessonPlanFavourite(data: FavoriteLessonPlanRequest) {
    return ApiRequestMixin.request("/api/history/favourites/lesson-plans", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(data),
    });
  },

  /**
   * Remove activity favourite
   */
  async removeActivityFavourite(activityId: string) {
    return ApiRequestMixin.request(
      `/api/history/favourites/activities/${activityId}`,
      {
        method: "DELETE",
      },
    );
  },

  /**
   * Delete favourite (by favourite ID)
   */
  async deleteFavourite(favouriteId: string) {
    return ApiRequestMixin.request(`/api/history/favourites/${favouriteId}`, {
      method: "DELETE",
    });
  },
};
