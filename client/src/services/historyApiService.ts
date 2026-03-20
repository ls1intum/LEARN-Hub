import type {
  FavoriteActivityRequest,
  FavoriteLessonPlanRequest,
  ActivityFavoritesResponse,
  LessonPlanFavoritesResponse,
  FavoriteStatusResponse,
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
   * Get activity favourites
   */
  async getActivityFavourites(limit = 50, offset = 0) {
    return ApiRequestMixin.request<ActivityFavoritesResponse>(
      `/api/history/favourites/activities?limit=${limit}&offset=${offset}`,
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
    return ApiRequestMixin.request(
      `/api/history/favourites/${favouriteId}`,
      {
        method: "DELETE",
      },
    );
  },

  /**
   * Check if activity is favourited
   */
  async checkActivityFavouriteStatus(activityId: string) {
    return ApiRequestMixin.request<FavoriteStatusResponse>(
      `/api/history/favourites/activities/${activityId}/status`,
    );
  },
};
