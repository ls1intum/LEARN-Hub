import { describe, it, expect, vi, beforeEach } from "vitest";
import { secureStorage } from "../secureStorage";

// Mock sessionStorage
const sessionStorageMock = {
  getItem: vi.fn(),
  setItem: vi.fn(),
  removeItem: vi.fn(),
  clear: vi.fn(),
  length: 0,
  key: vi.fn(),
};

Object.defineProperty(window, "sessionStorage", {
  value: sessionStorageMock,
});

describe("secureStorage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe("Token Management", () => {
    it("should store and retrieve access token", () => {
      const token = "test_access_token";
      secureStorage.setAccessToken(token);

      expect(sessionStorageMock.setItem).toHaveBeenCalledWith(
        "learn_hub_access_token",
        token,
      );

      sessionStorageMock.getItem.mockReturnValue(token);
      expect(secureStorage.getAccessToken()).toBe(token);
    });

    it("should store and retrieve refresh token", () => {
      const token = "test_refresh_token";
      secureStorage.setRefreshToken(token);

      expect(sessionStorageMock.setItem).toHaveBeenCalledWith(
        "learn_hub_refresh_token",
        token,
      );

      sessionStorageMock.getItem.mockReturnValue(token);
      expect(secureStorage.getRefreshToken()).toBe(token);
    });

    it("should store both tokens together", () => {
      const accessToken = "test_access_token";
      const refreshToken = "test_refresh_token";

      secureStorage.setTokens(accessToken, refreshToken);

      expect(sessionStorageMock.setItem).toHaveBeenCalledWith(
        "learn_hub_access_token",
        accessToken,
      );
      expect(sessionStorageMock.setItem).toHaveBeenCalledWith(
        "learn_hub_refresh_token",
        refreshToken,
      );
    });

    it("should clear all tokens", () => {
      secureStorage.clearTokens();

      expect(sessionStorageMock.removeItem).toHaveBeenCalledWith(
        "learn_hub_access_token",
      );
      expect(sessionStorageMock.removeItem).toHaveBeenCalledWith(
        "learn_hub_refresh_token",
      );
    });
  });

  describe("Storage Availability", () => {
    it("should handle storage unavailable gracefully", () => {
      // Mock sessionStorage to throw an error
      sessionStorageMock.setItem.mockImplementation(() => {
        throw new Error("Storage not available");
      });

      // Should not throw an error
      expect(() => {
        secureStorage.setAccessToken("test_token");
      }).not.toThrow();
    });

    it("should return null when storage is unavailable", () => {
      // Mock sessionStorage to throw an error
      sessionStorageMock.getItem.mockImplementation(() => {
        throw new Error("Storage not available");
      });

      expect(secureStorage.getAccessToken()).toBe(null);
    });
  });

  describe("Token Operations", () => {
    it("should handle token operations without throwing errors", () => {
      // Should not throw an error even with invalid token
      expect(() => {
        secureStorage.setAccessToken("invalid_token");
      }).not.toThrow();

      expect(() => {
        secureStorage.setRefreshToken("invalid_token");
      }).not.toThrow();

      expect(() => {
        secureStorage.setTokens("access", "refresh");
      }).not.toThrow();
    });
  });
});
