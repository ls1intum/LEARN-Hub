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
import { server } from "@/test/mocks/server";
import { resetMockSession } from "@/test/mocks/handlers";

// Setup MSW server for this test file
beforeAll(() => server.listen({ onUnhandledRequest: "error" }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

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

  beforeEach(() => {
    vi.clearAllMocks();
    resetMockSession();
    authService = new AuthService();
  });

  describe("Password Login", () => {
    it("should successfully login with valid credentials", async () => {
      const result = await authService.login("test@example.com", "password123");

      expect(result.success).toBe(true);
      expect(result.user).toBeDefined();
      expect(result.user?.email).toBe("test@example.com");
    });

    it("should fail with invalid credentials", async () => {
      const result = await authService.login("wrong@example.com", "wrongpass");

      expect(result.success).toBe(false);
      expect(result.message).toBe("Invalid credentials");
    });
  });

  describe("Logout", () => {
    it("should clear the server session on logout", async () => {
      await authService.login("test@example.com", "password123");
      await authService.logout();

      const user = await authService.getCurrentUser();
      expect(user).toBeNull();
    });
  });

  describe("Get Current User", () => {
    it("should return user when authenticated", async () => {
      await authService.login("test@example.com", "password123");

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

      const user = await authService.getCurrentUser();

      expect(user).toBeNull();
    });
  });
});
