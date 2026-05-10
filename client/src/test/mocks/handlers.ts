import { http, HttpResponse } from "msw";

/**
 * Mock API handlers for testing
 * These handlers simulate server responses for common API endpoints
 */
let currentUser: { id: string; email: string; role: string } | null = null;

export const resetMockSession = () => {
  currentUser = null;
};

export const handlers = [
  http.get("/api/auth/csrf", () => {
    return HttpResponse.json(
      { message: "csrf-token" },
      {
        headers: {
          "Set-Cookie": "XSRF-TOKEN=csrf-token; Path=/",
        },
      },
    );
  }),

  // Authentication endpoints
  http.post("/api/auth/login", async ({ request }) => {
    const body = (await request.json()) as { email: string; password: string };

    if (body.email === "admin@example.com" && body.password === "password123") {
      currentUser = {
        id: "99",
        email: "admin@example.com",
        role: "ADMIN",
      };
      return HttpResponse.json({
        user: currentUser,
      });
    }

    if (body.email === "test@example.com" && body.password === "password123") {
      currentUser = {
        id: "1",
        email: "test@example.com",
        role: "TEACHER",
      };
      return HttpResponse.json({
        user: currentUser,
      });
    }

    return HttpResponse.json({ error: "Invalid credentials" }, { status: 401 });
  }),

  http.post("/api/auth/logout", () => {
    currentUser = null;
    return HttpResponse.json({ message: "Logged out successfully" });
  }),

  http.get("/api/auth/me", () => {
    if (!currentUser) {
      return HttpResponse.json({ error: "Unauthorized" }, { status: 401 });
    }

    return HttpResponse.json(currentUser);
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
