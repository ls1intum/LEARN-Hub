import type { User } from "@/types/activity";
import type {
  UserRequest,
  UsersResponse,
  UpdateProfileRequest,
} from "@/types/api";
import { ApiRequestMixin } from "./apiService";

/**
 * User/auth-related API methods: current user, admin user management, profiles.
 */
export const UserApi = {
  /**
   * Get current user
   */
  async getCurrentUser() {
    return ApiRequestMixin.request<User>("/api/auth/me");
  },

  /**
   * Get users (admin only)
   */
  async getUsers() {
    return ApiRequestMixin.request<UsersResponse>("/api/auth/users");
  },

  /**
   * Create user (admin only)
   */
  async createUser(data: UserRequest) {
    return ApiRequestMixin.request("/api/auth/users", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(data),
    });
  },

  /**
   * Update user (admin only)
   */
  async updateUser(userId: number, data: UserRequest) {
    return ApiRequestMixin.request(`/api/auth/users/${userId}`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(data),
    });
  },

  /**
   * Delete user (admin only)
   */
  async deleteUser(userId: number) {
    return ApiRequestMixin.request(`/api/auth/users/${userId}`, {
      method: "DELETE",
    });
  },

  /**
   * Update current user's profile
   */
  async updateProfile(data: UpdateProfileRequest) {
    return ApiRequestMixin.request("/api/auth/me", {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(data),
    });
  },

  /**
   * Delete current user's account
   */
  async deleteProfile() {
    return ApiRequestMixin.request("/api/auth/me", {
      method: "DELETE",
    });
  },
};
