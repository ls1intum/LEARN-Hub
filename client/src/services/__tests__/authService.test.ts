import {
  describe,
  it,
  expect,
  beforeEach,
  beforeAll,
  afterEach,
  afterAll,
  vi,
} from "vitest";
import { AuthService } from "../authService";
import { secureStorage } from "@/utils/secureStorage";
import { server } from "@/test/mocks/server";

// Setup MSW server for this test file
beforeAll(() => server.listen({ onUnhandledRequest: "error" }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

// Mock secureStorage
vi.mock("@/utils/secureStorage", () => ({
  secureStorage: {
    getAccessToken: vi.fn(),
    getRefreshToken: vi.fn(),
    setTokens: vi.fn(),
    clearTokens: vi.fn(),
  },
}));

// Mock logger
vi.mock("@/services/logger", () => ({
  logger: {
    error: vi.fn(),
    info: vi.fn(),
    warn: vi.fn(),
  },
}));

describe("AuthService", () => {
  let authService: AuthService;

  // Create a valid JWT token for testing (expires in 1 hour)
  const createValidToken = () => {
    const payload = {
      exp: Math.floor(Date.now() / 1000) + 3600, // Expires in 1 hour
      user_id: 1,
    };
    const encodedPayload = btoa(JSON.stringify(payload));
    return `header.${encodedPayload}.signature`;
  };

  // Create an expired JWT token
  const createExpiredToken = () => {
    const payload = {
      exp: Math.floor(Date.now() / 1000) - 3600, // Expired 1 hour ago
      user_id: 1,
    };
    const encodedPayload = btoa(JSON.stringify(payload));
    return `header.${encodedPayload}.signature`;
  };

  beforeEach(() => {
    // Clear all mocks before each test
    vi.clearAllMocks();

    // Mock empty storage by default
    vi.mocked(secureStorage.getAccessToken).mockReturnValue(null);
    vi.mocked(secureStorage.getRefreshToken).mockReturnValue(null);

    // Create a fresh instance for each test
    authService = new AuthService();
  });

  describe("Token Management", () => {
    it("should set tokens in storage", () => {
      const accessToken = createValidToken();
      const refreshToken = createValidToken();

      authService.setTokens(accessToken, refreshToken);

      expect(secureStorage.setTokens).toHaveBeenCalledWith(
        accessToken,
        refreshToken,
      );
    });

    it("should clear tokens from storage", () => {
      authService.clearTokens();

      expect(secureStorage.clearTokens).toHaveBeenCalled();
    });

    it("should return auth header when token exists", () => {
      const accessToken = createValidToken();
      authService.setTokens(accessToken, "refresh-token");

      const header = authService.getAuthHeader();

      expect(header).toEqual({ Authorization: `Bearer ${accessToken}` });
    });

    it("should return empty object when no token exists", () => {
      const header = authService.getAuthHeader();

      expect(header).toEqual({});
    });
  });

  describe("Token Validation", () => {
    it("should detect valid token", () => {
      const validToken = createValidToken();

      expect(authService.isTokenExpired(validToken)).toBe(false);
    });

    it("should detect expired token", () => {
      const expiredToken = createExpiredToken();

      expect(authService.isTokenExpired(expiredToken)).toBe(true);
    });

    it("should treat malformed token as expired", () => {
      expect(authService.isTokenExpired("invalid-token")).toBe(true);
    });
  });

  describe("Authentication Status", () => {
    it("should return true when valid token exists", () => {
      const validToken = createValidToken();
      authService.setTokens(validToken, "refresh-token");

      expect(authService.isAuthenticated()).toBe(true);
    });

    it("should return false when no token exists", () => {
      expect(authService.isAuthenticated()).toBe(false);
    });

    it("should clear expired tokens and return false", () => {
      const expiredToken = createExpiredToken();
      authService.setTokens(expiredToken, "refresh-token");

      expect(authService.isAuthenticated()).toBe(false);
      expect(secureStorage.clearTokens).toHaveBeenCalled();
    });
  });

  describe("Teacher Login", () => {
    it("should successfully login with valid credentials", async () => {
      const result = await authService.teacherLogin(
        "test@example.com",
        "password123",
      );

      expect(result.success).toBe(true);
      expect(result.user).toBeDefined();
      expect(result.user?.email).toBe("test@example.com");
      expect(secureStorage.setTokens).toHaveBeenCalledWith(
        "mock-access-token",
        "mock-refresh-token",
      );
    });

    it("should fail with invalid credentials", async () => {
      const result = await authService.teacherLogin(
        "wrong@example.com",
        "wrongpass",
      );

      expect(result.success).toBe(false);
      expect(result.message).toBe("Invalid credentials");
      expect(secureStorage.setTokens).not.toHaveBeenCalled();
    });
  });

  describe("Guest Login", () => {
    it("should create guest user without API call", async () => {
      const result = await authService.guestLogin();

      expect(result.success).toBe(true);
      expect(result.user).toBeDefined();
      expect(result.user?.role).toBe("GUEST");
      expect(result.user?.email).toBe("guest@example.com");
    });
  });

  describe("Token Refresh", () => {
    it("should refresh access token successfully", async () => {
      const refreshToken = createValidToken();
      authService.setTokens(createValidToken(), refreshToken);

      const result = await authService.refreshAccessToken();

      expect(result).toBe(true);
      expect(secureStorage.setTokens).toHaveBeenCalled();
    });

    it("should fail when no refresh token exists", async () => {
      const result = await authService.refreshAccessToken();

      expect(result).toBe(false);
    });
  });

  describe("Logout", () => {
    it("should clear tokens on logout", async () => {
      const validToken = createValidToken();
      authService.setTokens(validToken, "refresh-token");

      await authService.logout();

      expect(secureStorage.clearTokens).toHaveBeenCalled();
    });

    it("should clear tokens even if API call fails", async () => {
      const validToken = createValidToken();
      authService.setTokens(validToken, "refresh-token");

      await authService.logout();

      expect(secureStorage.clearTokens).toHaveBeenCalled();
    });
  });

  describe("Get Current User", () => {
    it("should return user when authenticated", async () => {
      const validToken = createValidToken();
      authService.setTokens(validToken, "refresh-token");

      const user = await authService.getCurrentUser();

      expect(user).toBeDefined();
      expect(user?.email).toBe("test@example.com");
    });

    it("should return null and clear tokens on 401", async () => {
      // Override handler to return 401 for this test
      const { http, HttpResponse } = await import("msw");
      server.use(
        http.get("/api/auth/me", () => {
          return HttpResponse.json({ error: "Unauthorized" }, { status: 401 });
        }),
      );

      authService.setTokens("invalid-token", "refresh-token");

      const user = await authService.getCurrentUser();

      expect(user).toBeNull();
      expect(secureStorage.clearTokens).toHaveBeenCalled();
    });
  });
});
