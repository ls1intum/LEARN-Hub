// Authentication service for handling cookie-based sessions
import { logger } from "@/services/logger";

export interface User {
  id: string;
  email: string;
  firstName?: string;
  lastName?: string;
  role: "ADMIN" | "TEACHER" | "GUEST";
}

export interface AuthResponse {
  success: boolean;
  message?: string;
  user?: User;
}

export class AuthService {
  private baseURL: string;
  private csrfRequest: Promise<void> | null = null;
  private static readonly SESSION_FLAG = "learnhub.hasSession";

  constructor() {
    this.baseURL = import.meta.env.VITE_API_SERVER || "";
  }

  private markSessionActive(): void {
    localStorage.setItem(AuthService.SESSION_FLAG, "1");
  }

  private clearSessionFlag(): void {
    localStorage.removeItem(AuthService.SESSION_FLAG);
  }

  hasSession(): boolean {
    return localStorage.getItem(AuthService.SESSION_FLAG) === "1";
  }

  private getCookie(name: string): string | null {
    if (typeof document === "undefined") {
      return null;
    }

    const cookies = document.cookie ? document.cookie.split("; ") : [];
    for (const cookie of cookies) {
      const [cookieName, ...cookieValue] = cookie.split("=");
      if (cookieName === name) {
        return decodeURIComponent(cookieValue.join("="));
      }
    }

    return null;
  }

  private async ensureCsrfToken(): Promise<void> {
    if (this.getCookie("XSRF-TOKEN")) {
      return;
    }

    if (!this.csrfRequest) {
      this.csrfRequest = fetch(this.baseURL + "/api/auth/csrf", {
        method: "GET",
        credentials: "include",
      })
        .then(() => undefined)
        .finally(() => {
          this.csrfRequest = null;
        });
    }

    await this.csrfRequest;
  }

  private async createHeaders(
    options: RequestInit,
  ): Promise<Record<string, string>> {
    const headers: Record<string, string> = {
      ...(options.headers as Record<string, string> | undefined),
    };

    const method = (options.method || "GET").toUpperCase();
    const isMutatingRequest = !["GET", "HEAD", "OPTIONS"].includes(method);

    if (isMutatingRequest) {
      await this.ensureCsrfToken();

      const csrfToken = this.getCookie("XSRF-TOKEN");
      if (csrfToken) {
        headers["X-XSRF-TOKEN"] = csrfToken;
      }
    }

    if (!(options.body instanceof FormData) && !headers["Content-Type"]) {
      headers["Content-Type"] = "application/json";
    }

    return headers;
  }

  clearTokens(): void {
    this.clearSessionFlag();
  }

  async makeAuthenticatedRequest(
    url: string,
    options: RequestInit = {},
  ): Promise<Response> {
    const headers = await this.createHeaders(options);

    const fullUrl = this.baseURL + url;
    return fetch(fullUrl, {
      ...options,
      headers,
      credentials: "include",
    });
  }

  private async performLogin(
    endpoint: string,
    body: Record<string, string | number | boolean>,
    errorMessage: string = "Login failed",
  ): Promise<AuthResponse> {
    try {
      const response = await fetch(this.baseURL + endpoint, {
        method: "POST",
        headers: await this.createHeaders({ method: "POST" }),
        credentials: "include",
        body: JSON.stringify(body),
      });

      if (response.ok) {
        const responseData = await response.json();
        this.markSessionActive();
        return { success: true, user: responseData.user ?? responseData };
      } else {
        const errorData = await response.json();
        return { success: false, message: errorData.error || errorMessage };
      }
    } catch (error) {
      logger.error("Error logging in", error, "AuthService");
      return { success: false, message: "An unexpected error occurred" };
    }
  }

  async login(email: string, password: string): Promise<AuthResponse> {
    return this.performLogin("/api/auth/login", { email, password });
  }

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

  async requestVerificationCode(email: string): Promise<AuthResponse> {
    try {
      const response = await fetch(
        this.baseURL + "/api/auth/verification-code",
        {
          method: "POST",
          headers: await this.createHeaders({ method: "POST" }),
          credentials: "include",
          body: JSON.stringify({
            email: email,
          }),
        },
      );

      const responseData = await response.json();
      return {
        success: response.ok,
        message: responseData.message || responseData.error,
      };
    } catch (error) {
      logger.error("Error requesting verification code", error, "AuthService");
      return { success: false, message: "Temporary problem with email login" };
    }
  }

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
          headers: await this.createHeaders({ method: "POST" }),
          credentials: "include",
          body: JSON.stringify({
            email: email,
            firstName: firstName,
            lastName: lastName,
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

  async resetPassword(email: string): Promise<AuthResponse> {
    try {
      const response = await fetch(this.baseURL + "/api/auth/reset-password", {
        method: "POST",
        headers: await this.createHeaders({ method: "POST" }),
        credentials: "include",
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

  async logout(): Promise<void> {
    this.clearSessionFlag();
    try {
      await this.makeAuthenticatedRequest("/api/auth/logout", {
        method: "POST",
      });
    } catch (error) {
      logger.error("Error logging out", error, "AuthService");
    }
  }

  async getCurrentUser(): Promise<User | null> {
    if (!this.hasSession()) {
      return null;
    }
    try {
      const response = await this.makeAuthenticatedRequest("/api/auth/me");
      if (response.ok) {
        const responseData = await response.json();
        return responseData;
      }
      // Session expired or invalid server-side — clear the local flag
      this.clearSessionFlag();
      return null;
    } catch (error) {
      logger.error("Error getting current user", error, "AuthService");
      return null;
    }
  }
}

// Create singleton instance
export const authService = new AuthService();
