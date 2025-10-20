import { describe, it, expect, vi, beforeEach } from "vitest";
import { AuthService } from "../authService";
import { secureStorage } from "@/utils/secureStorage";

// Mock secureStorage
vi.mock("@/utils/secureStorage", () => ({
  secureStorage: {
    getAccessToken: vi.fn(),
    getRefreshToken: vi.fn(),
    setAccessToken: vi.fn(),
    setRefreshToken: vi.fn(),
    setTokens: vi.fn(),
    clearTokens: vi.fn(),
  },
}));

// Mock logger
vi.mock("@/services/logger", () => ({
  logger: {
    error: vi.fn(),
    info: vi.fn(),
  },
}));

describe("AuthService", () => {
  let authService: AuthService;

  beforeEach(() => {
    vi.clearAllMocks();
    authService = new AuthService();
  });

  describe("Token Management", () => {
    it("should clear all tokens", () => {
      authService.clearTokens();

      expect(secureStorage.clearTokens).toHaveBeenCalled();
    });

    it("should check if user is not authenticated when no token", () => {
      vi.mocked(secureStorage.getAccessToken).mockReturnValue(null);

      expect(authService.isAuthenticated()).toBe(false);
    });
  });

  describe("Token Validation", () => {
    it("should detect expired token", () => {
      // Create a JWT token that's expired (exp: 0 = Jan 1, 1970)
      const expiredToken =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHAiOjB9.invalid";

      expect(authService.isTokenExpired(expiredToken)).toBe(true);
    });

    it("should detect valid token", () => {
      // Create a JWT token that's valid for 1 hour from now
      const futureTime = Math.floor(Date.now() / 1000) + 3600;
      const payload = JSON.stringify({ exp: futureTime });
      const encodedPayload = btoa(payload);
      const validToken = `eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.${encodedPayload}.invalid`;

      expect(authService.isTokenExpired(validToken)).toBe(false);
    });

    it("should handle invalid token format", () => {
      expect(authService.isTokenExpired("invalid.token")).toBe(true);
      expect(authService.isTokenExpired("not-a-jwt")).toBe(true);
    });
  });
});
