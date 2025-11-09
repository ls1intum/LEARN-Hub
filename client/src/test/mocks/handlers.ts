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

    if (body.email === "test@example.com" && body.password === "password123") {
      return HttpResponse.json({
        access_token: "mock-access-token",
        refresh_token: "mock-refresh-token",
        user: {
          id: 1,
          email: "test@example.com",
          role: "TEACHER",
        },
      });
    }

    return HttpResponse.json({ error: "Invalid credentials" }, { status: 401 });
  }),

  http.post("/api/auth/teacher/login", async ({ request }) => {
    const body = (await request.json()) as { email: string; password: string };

    if (
      body.email === "teacher@example.com" &&
      body.password === "teacher123"
    ) {
      return HttpResponse.json({
        access_token: "mock-teacher-token",
        refresh_token: "mock-refresh-token",
        user: {
          id: 2,
          email: "teacher@example.com",
          role: "TEACHER",
        },
      });
    }

    return HttpResponse.json({ error: "Invalid credentials" }, { status: 401 });
  }),

  http.post("/api/auth/guest/login", () => {
    return HttpResponse.json({
      access_token: "mock-guest-token",
      refresh_token: "mock-guest-refresh-token",
      user: {
        id: 999,
        email: "guest@temporary.com",
        role: "GUEST",
      },
    });
  }),

  http.post("/api/auth/refresh", async ({ request }) => {
    const body = (await request.json()) as { refresh_token: string };

    if (body.refresh_token) {
      return HttpResponse.json({
        access_token: "new-mock-access-token",
        refresh_token: body.refresh_token,
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
          bloom_level: "apply",
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
      bloom_level: "apply",
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
