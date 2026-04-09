import { http, HttpResponse } from "msw";

/**
 * Mock API handlers for testing
 * These handlers simulate server responses for common API endpoints
 * Using relative URLs to match the authService behavior
 */
export const handlers = [
  // Authentication endpoints
  http.post("/api/auth/login", async ({ request }) => {
    const body = (await request.json()) as { email: string; password: string };

    if (body.email === "admin@example.com" && body.password === "password123") {
      return HttpResponse.json({
        accessToken: "mock-admin-token",
        refreshToken: "mock-refresh-token",
        user: {
          id: 99,
          email: "admin@example.com",
          role: "ADMIN",
        },
      });
    }

    if (body.email === "test@example.com" && body.password === "password123") {
      return HttpResponse.json({
        accessToken: "mock-access-token",
        refreshToken: "mock-refresh-token",
        user: {
          id: 1,
          email: "test@example.com",
          role: "TEACHER",
        },
      });
    }

    return HttpResponse.json({ error: "Invalid credentials" }, { status: 401 });
  }),

  http.post("/api/auth/guest/login", () => {
    return HttpResponse.json({
      accessToken: "mock-guest-token",
      refreshToken: "mock-guest-refresh-token",
      user: {
        id: 999,
        email: "guest@temporary.com",
        role: "GUEST",
      },
    });
  }),

  http.post("/api/auth/refresh", async ({ request }) => {
    const body = (await request.json()) as { refreshToken: string };

    if (body.refreshToken) {
      return HttpResponse.json({
        accessToken: "new-mock-access-token",
        refreshToken: body.refreshToken,
      });
    }

    return HttpResponse.json(
      { error: "Invalid refresh token" },
      { status: 401 },
    );
  }),

  http.post("/api/auth/logout", () => {
    return HttpResponse.json({ message: "Logged out successfully" });
  }),

  http.get("/api/auth/me", ({ request }) => {
    const authHeader = request.headers.get("Authorization");

    if (!authHeader || !authHeader.startsWith("Bearer ")) {
      return HttpResponse.json({ error: "Unauthorized" }, { status: 401 });
    }

    return HttpResponse.json({
      id: 1,
      email: "test@example.com",
      role: "TEACHER",
    });
  }),

  // Activities endpoints
  http.get("/api/activities", () => {
    return HttpResponse.json({
      activities: [
        {
          id: 1,
          title: "Test Activity 1",
          description: "Test description",
          format: "unplugged",
          bloomLevel: "apply",
        },
      ],
      total: 1,
    });
  }),

  http.get("/api/activities/:id", ({ params }) => {
    const { id } = params;
    return HttpResponse.json({
      id: Number(id),
      title: `Test Activity ${id}`,
      description: "Test description",
      format: "unplugged",
      bloomLevel: "apply",
    });
  }),

  // Recommendations endpoint
  http.post("/api/recommendations", () => {
    return HttpResponse.json({
      recommendations: [
        {
          id: 1,
          title: "Recommended Activity",
          score: 0.95,
        },
      ],
    });
  }),

  // Meta endpoint
  http.get("/api/meta/environment", () => {
    return HttpResponse.json({
      environment: "test",
      version: "1.0.0",
    });
  }),
];
