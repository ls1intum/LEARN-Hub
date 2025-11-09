// Authentication service for handling JWT tokens
import { secureStorage } from "@/utils/secureStorage";
import { logger } from "@/services/logger";

export interface User {
  id: number;
  email: string;
  first_name?: string;
  last_name?: string;
  role: "ADMIN" | "TEACHER" | "GUEST";
}

export interface AuthTokens {
  access_token: string;
  refresh_token: string;
}

export interface AuthResponse {
  success: boolean;
  message?: string;
  user?: User;
}

export class AuthService {
  private accessToken: string | null = null;
  private refreshToken: string | null = null;
  private baseURL: string;

  constructor() {
    this.loadTokensFromStorage();
    this.validateAndCleanTokens();
    // Use API_SERVER environment variable if available, otherwise use relative URLs
    this.baseURL = import.meta.env.VITE_API_SERVER || "";
  }

  private loadTokensFromStorage(): void {
    this.accessToken = secureStorage.getAccessToken();
    this.refreshToken = secureStorage.getRefreshToken();
  }

  private validateAndCleanTokens(): void {
    if (this.accessToken && this.isTokenExpired(this.accessToken)) {
      this.clearTokens();
    }
  }

  isTokenExpired(token: string): boolean {
    try {
      const payload = JSON.parse(atob(token.split(".")[1]));
      const now = Math.floor(Date.now() / 1000);
      return payload.exp <= now;
    } catch {
      return true; // If we can't decode, consider it expired
    }
  }

  // Store tokens in secure storage
  setTokens(accessToken: string, refreshToken: string): void {
    this.accessToken = accessToken;
    this.refreshToken = refreshToken;
    secureStorage.setTokens(accessToken, refreshToken);
  }

  // Clear tokens from secure storage
  clearTokens(): void {
    this.accessToken = null;
    this.refreshToken = null;
    secureStorage.clearTokens();
  }

  // Get authorization header for API requests
  getAuthHeader(): Record<string, string> {
    if (this.accessToken) {
      return { Authorization: `Bearer ${this.accessToken}` };
    }
    return {};
  }

  // Check if user is authenticated
  isAuthenticated(): boolean {
    if (!this.accessToken) {
      return false;
    }

    if (this.isTokenExpired(this.accessToken)) {
      this.clearTokens();
      return false;
    }

    return true;
  }

  // Make authenticated API request
  async makeAuthenticatedRequest(
    url: string,
    options: RequestInit = {},
  ): Promise<Response> {
    const headers: Record<string, string> = {
      ...this.getAuthHeader(),
      ...(options.headers as Record<string, string>),
    };

    // Only set Content-Type for JSON requests, not for FormData
    if (!(options.body instanceof FormData)) {
      headers["Content-Type"] = "application/json";
    }

    const fullUrl = this.baseURL + url;
    let response = await fetch(fullUrl, { ...options, headers });

    // If access token is expired, try to refresh
    if (response.status === 401 && this.refreshToken) {
      const refreshed = await this.refreshAccessToken();
      if (refreshed) {
        // Retry the original request with new token
        headers["Authorization"] = `Bearer ${this.accessToken}`;
        response = await fetch(fullUrl, { ...options, headers });
      } else {
        // Refresh failed, clear tokens
        this.clearTokens();
        // Return the 401 response instead of throwing
        return response;
      }
    }

    return response;
  }

  // Refresh access token using refresh token
  async refreshAccessToken(): Promise<boolean> {
    if (!this.refreshToken) {
      return false;
    }

    try {
      const response = await fetch(this.baseURL + "/api/auth/refresh", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          refresh_token: this.refreshToken,
        }),
      });

      if (response.ok) {
        const responseData = await response.json();
        // Server returns data directly without wrapper
        this.setTokens(responseData.access_token, responseData.refresh_token);
        return true;
      } else {
        this.clearTokens();
        return false;
      }
    } catch (error) {
      logger.error("Error refreshing token", error, "AuthService");
      this.clearTokens();
      return false;
    }
  }

  // Generic login method
  private async performLogin(
    endpoint: string,
    body: Record<string, string | number | boolean>,
    errorMessage: string = "Login failed",
  ): Promise<AuthResponse> {
    try {
      const response = await fetch(this.baseURL + endpoint, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(body),
      });

      if (response.ok) {
        const responseData = await response.json();
        // Server returns data directly without wrapper
        this.setTokens(responseData.access_token, responseData.refresh_token);
        return { success: true, user: responseData.user };
      } else {
        const errorData = await response.json();
        return { success: false, message: errorData.error || errorMessage };
      }
    } catch (error) {
      logger.error("Error logging in", error, "AuthService");
      return { success: false, message: "An unexpected error occurred" };
    }
  }

  // Login with admin credentials
  async adminLogin(email: string, password: string): Promise<AuthResponse> {
    return this.performLogin("/api/auth/admin/login", { email, password });
  }

  // Teacher login with password
  async teacherLogin(email: string, password: string): Promise<AuthResponse> {
    return this.performLogin("/api/auth/login", { email, password });
  }

  // Login with verification code
  async verificationCodeLogin(
    code: string,
    email: string,
  ): Promise<AuthResponse> {
    return this.performLogin(
      "/api/auth/verify",
      { code, email },
      "Invalid or expired code",
    );
  }

  // Request verification code
  async requestVerificationCode(email: string): Promise<AuthResponse> {
    try {
      const response = await fetch(
        this.baseURL + "/api/auth/verification-code",
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            email: email,
          }),
        },
      );

      const responseData = await response.json();
      // For verification code request, the server returns success/message directly
      return {
        success: response.ok,
        message: responseData.message || responseData.error,
      };
    } catch (error) {
      logger.error("Error requesting verification code", error, "AuthService");
      return { success: false, message: "Temporary problem with email login" };
    }
  }

  // Register new teacher
  async registerTeacher(
    email: string,
    firstName: string,
    lastName: string,
  ): Promise<AuthResponse> {
    try {
      const response = await fetch(
        this.baseURL + "/api/auth/register-teacher",
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            email: email,
            first_name: firstName,
            last_name: lastName,
          }),
        },
      );

      const responseData = await response.json();
      return {
        success: response.ok,
        message: responseData.message || responseData.error,
        user: responseData.user,
      };
    } catch (error) {
      logger.error("Error registering teacher", error, "AuthService");
      return { success: false, message: "An unexpected error occurred" };
    }
  }

  // Reset teacher password
  async resetPassword(email: string): Promise<AuthResponse> {
    try {
      const response = await fetch(this.baseURL + "/api/auth/reset-password", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          email: email,
        }),
      });

      const responseData = await response.json();
      return {
        success: response.ok,
        message: responseData.message || responseData.error,
      };
    } catch (error) {
      logger.error("Error resetting password", error, "AuthService");
      return { success: false, message: "An unexpected error occurred" };
    }
  }

  // Logout
  async logout(): Promise<void> {
    try {
      await fetch("/api/auth/logout", {
        method: "POST",
        headers: this.getAuthHeader(),
      });
    } catch (error) {
      logger.error("Error logging out", error, "AuthService");
    } finally {
      this.clearTokens();
    }
  }

  // Login as guest
  async guestLogin(): Promise<AuthResponse> {
    try {
      // Create a guest user object without making any API calls
      const guestUser: User = {
        id: 0,
        email: "guest@example.com",
        role: "GUEST",
      };

      return { success: true, user: guestUser };
    } catch (error) {
      logger.error("Error logging in as guest", error, "AuthService");
      return { success: false, message: "An unexpected error occurred" };
    }
  }

  // Get current user info
  async getCurrentUser(): Promise<User | null> {
    try {
      const response = await this.makeAuthenticatedRequest("/api/auth/me");
      if (response.ok) {
        const responseData = await response.json();
        // Server returns data directly without wrapper
        return responseData;
      }
      // If we get a 401, tokens are invalid, clear them
      if (response.status === 401) {
        this.clearTokens();
      }
      return null;
    } catch (error) {
      logger.error("Error getting current user", error, "AuthService");
      return null;
    }
  }
}

// Create singleton instance
export const authService = new AuthService();
