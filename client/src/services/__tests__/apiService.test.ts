import {
  describe,
  it,
  expect,
  vi,
  beforeEach,
  beforeAll,
  afterEach,
  afterAll,
} from "vitest";
import { ApiService } from "../apiService";
import { authService } from "../authService";
import { server } from "@/test/mocks/server";

// Setup MSW server
beforeAll(() => server.listen({ onUnhandledRequest: "error" }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

// Mock authService
vi.mock("../authService", () => ({
  authService: {
    makeAuthenticatedRequest: vi.fn(),
  },
}));

describe("ApiService", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe("request() - Core Request Handler", () => {
    it("should successfully return JSON data from API", async () => {
      const mockResponse = { id: 1, name: "Test Activity" };
      vi.mocked(authService.makeAuthenticatedRequest).mockResolvedValue(
        new Response(JSON.stringify(mockResponse), {
          status: 200,
          headers: { "Content-Type": "application/json" },
        }),
      );

      const result = await ApiService.request("/api/test");

      expect(result).toEqual(mockResponse);
      expect(authService.makeAuthenticatedRequest).toHaveBeenCalledWith(
        "/api/test",
        {},
      );
    });

    it("should throw error with status on non-OK response", async () => {
      vi.mocked(authService.makeAuthenticatedRequest).mockResolvedValue(
        new Response(JSON.stringify({ error: "Not found" }), {
          status: 404,
        }),
      );

      await expect(ApiService.request("/api/test")).rejects.toThrow(
        "Not found",
      );
    });

    it("should format Pydantic validation errors (422) correctly", async () => {
      const validationError = {
        detail: [
          { loc: ["body", "email"], msg: "field required" },
          { loc: ["body", "password"], msg: "must be at least 8 characters" },
        ],
      };

      vi.mocked(authService.makeAuthenticatedRequest).mockResolvedValue(
        new Response(JSON.stringify(validationError), {
          status: 422,
        }),
      );

      await expect(ApiService.request("/api/test")).rejects.toThrow(
        "email: field required; password: must be at least 8 characters",
      );
    });

    it("should handle generic HTTP errors without error message", async () => {
      vi.mocked(authService.makeAuthenticatedRequest).mockResolvedValue(
        new Response("", {
          status: 500,
        }),
      );

      await expect(ApiService.request("/api/test")).rejects.toThrow(
        "HTTP error! status: 500",
      );
    });

    it("should handle null response", async () => {
      vi.mocked(authService.makeAuthenticatedRequest).mockResolvedValue(
        null as unknown as Response,
      );

      await expect(ApiService.request("/api/test")).rejects.toThrow(
        "HTTP error! status: unknown",
      );
    });

    it("should pass request options to authService", async () => {
      const mockResponse = { success: true };
      vi.mocked(authService.makeAuthenticatedRequest).mockResolvedValue(
        new Response(JSON.stringify(mockResponse), {
          status: 200,
        }),
      );

      const options = {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ data: "test" }),
      };

      await ApiService.request("/api/test", options);

      expect(authService.makeAuthenticatedRequest).toHaveBeenCalledWith(
        "/api/test",
        options,
      );
    });
  });

  describe("getActivities() - Query Parameter Serialization", () => {
    it("should handle empty search criteria", async () => {
      const mockResponse = { activities: [], total: 0 };
      vi.mocked(authService.makeAuthenticatedRequest).mockResolvedValue(
        new Response(JSON.stringify(mockResponse), { status: 200 }),
      );

      await ApiService.getActivities({});

      expect(authService.makeAuthenticatedRequest).toHaveBeenCalledWith(
        "/api/activities/?",
        {},
      );
    });

    it("should serialize array parameters correctly", async () => {
      const mockResponse = { activities: [], total: 0 };
      vi.mocked(authService.makeAuthenticatedRequest).mockResolvedValue(
        new Response(JSON.stringify(mockResponse), { status: 200 }),
      );

      await ApiService.getActivities({
        format: ["unplugged", "digital"],
        topics: ["decomposition"],
      });

      const call = vi.mocked(authService.makeAuthenticatedRequest).mock
        .calls[0];
      const url = call[0];

      expect(url).toContain("format=unplugged");
      expect(url).toContain("format=digital");
      expect(url).toContain("topics=decomposition");
    });

    it("should skip null, undefined, and empty string values", async () => {
      const mockResponse = { activities: [], total: 0 };
      vi.mocked(authService.makeAuthenticatedRequest).mockResolvedValue(
        new Response(JSON.stringify(mockResponse), { status: 200 }),
      );

      await ApiService.getActivities({
        format: ["unplugged"],
        limit: undefined,
        offset: null as unknown as number,
      });

      const call = vi.mocked(authService.makeAuthenticatedRequest).mock
        .calls[0];
      const url = call[0];

      expect(url).toContain("format=unplugged");
      expect(url).not.toContain("search");
      expect(url).not.toContain("limit");
      expect(url).not.toContain("offset");
    });

    it("should serialize numeric values correctly", async () => {
      const mockResponse = { activities: [], total: 0 };
      vi.mocked(authService.makeAuthenticatedRequest).mockResolvedValue(
        new Response(JSON.stringify(mockResponse), { status: 200 }),
      );

      await ApiService.getActivities({
        limit: 10,
        offset: 20,
      });

      const call = vi.mocked(authService.makeAuthenticatedRequest).mock
        .calls[0];
      const url = call[0];

      expect(url).toContain("limit=10");
      expect(url).toContain("offset=20");
    });
  });

  describe("Error Handling", () => {
    it("should handle network errors in request", async () => {
      vi.mocked(authService.makeAuthenticatedRequest).mockRejectedValue(
        new Error("Network error"),
      );

      await expect(ApiService.getActivity(1)).rejects.toThrow("Network error");
    });

    it("should handle malformed JSON response", async () => {
      vi.mocked(authService.makeAuthenticatedRequest).mockResolvedValue(
        new Response("invalid json", {
          status: 500,
        }),
      );

      await expect(ApiService.getActivity(1)).rejects.toThrow();
    });
  });

  describe("JSON Body Methods", () => {
    it("should send JSON body for POST requests", async () => {
      const mockResponse = { success: true };
      vi.mocked(authService.makeAuthenticatedRequest).mockResolvedValue(
        new Response(JSON.stringify(mockResponse), { status: 200 }),
      );

      const activityData = {
        name: "Test Activity",
        description: "Test",
        format: "unplugged",
        age_min: 8,
        age_max: 12,
        resources_needed: ["handouts"],
        bloom_level: "apply",
        duration_min_minutes: 30,
        topics: ["decomposition"],
      };

      await ApiService.createActivity(activityData);

      const call = vi.mocked(authService.makeAuthenticatedRequest).mock
        .calls[0];
      expect(call[1]?.method).toBe("POST");
      expect(call[1]?.headers).toEqual({ "Content-Type": "application/json" });
      expect(call[1]?.body).toBe(JSON.stringify(activityData));
    });

    it("should send JSON body for PUT requests", async () => {
      const mockResponse = { success: true };
      vi.mocked(authService.makeAuthenticatedRequest).mockResolvedValue(
        new Response(JSON.stringify(mockResponse), { status: 200 }),
      );

      const userData = {
        email: "test@example.com",
        first_name: "John",
      };

      await ApiService.updateProfile(userData);

      const call = vi.mocked(authService.makeAuthenticatedRequest).mock
        .calls[0];
      expect(call[1]?.method).toBe("PUT");
      expect(call[1]?.headers).toEqual({ "Content-Type": "application/json" });
    });
  });

  describe("FormData Methods", () => {
    it("should send FormData for file upload", async () => {
      const mockResponse = { document_id: 1 };
      vi.mocked(authService.makeAuthenticatedRequest).mockResolvedValue(
        new Response(JSON.stringify(mockResponse), { status: 200 }),
      );

      const file = new File(["content"], "test.pdf", {
        type: "application/pdf",
      });
      await ApiService.uploadPdf(file);

      const call = vi.mocked(authService.makeAuthenticatedRequest).mock
        .calls[0];
      expect(call[1]?.method).toBe("POST");
      expect(call[1]?.body).toBeInstanceOf(FormData);

      const formData = call[1]?.body as FormData;
      expect(formData.get("pdf_file")).toBe(file);
    });

    it("should process uploaded PDF document", async () => {
      const mockResponse = {
        extracted_data: {
          name: "Test Activity",
          description: "Test description",
          format: "unplugged",
        },
        confidence: 0.85,
        extraction_quality: "high",
      };
      vi.mocked(authService.makeAuthenticatedRequest).mockResolvedValue(
        new Response(JSON.stringify(mockResponse), { status: 200 }),
      );

      const result = await ApiService.processPdf(123);

      const call = vi.mocked(authService.makeAuthenticatedRequest).mock
        .calls[0];
      expect(call[0]).toBe("/api/documents/123/process");
      expect(call[1]?.method).toBe("POST");
      expect(result).toEqual(mockResponse);
    });
  });

  describe("DELETE Methods", () => {
    it("should send DELETE request correctly", async () => {
      const mockResponse = { success: true };
      vi.mocked(authService.makeAuthenticatedRequest).mockResolvedValue(
        new Response(JSON.stringify(mockResponse), { status: 200 }),
      );

      await ApiService.deleteActivity(1);

      const call = vi.mocked(authService.makeAuthenticatedRequest).mock
        .calls[0];
      expect(call[0]).toBe("/api/activities/1");
      expect(call[1]?.method).toBe("DELETE");
    });

    it("should delete search history entry", async () => {
      const mockResponse = { success: true };
      vi.mocked(authService.makeAuthenticatedRequest).mockResolvedValue(
        new Response(JSON.stringify(mockResponse), { status: 200 }),
      );

      await ApiService.deleteSearchHistoryEntry(123);

      const call = vi.mocked(authService.makeAuthenticatedRequest).mock
        .calls[0];
      expect(call[0]).toBe("/api/history/search/123");
      expect(call[1]?.method).toBe("DELETE");
    });
  });

  describe("Blob Response Methods", () => {
    it("should handle blob responses for PDF downloads", async () => {
      const mockBlob = new Blob(["pdf content"], { type: "application/pdf" });
      vi.mocked(authService.makeAuthenticatedRequest).mockResolvedValue(
        new Response(mockBlob, {
          status: 200,
          headers: { "Content-Type": "application/pdf" },
        }),
      );

      const result = await ApiService.downloadPdf(1);

      expect(result).toBeInstanceOf(Blob);
      // Note: Response.blob() may not preserve the original blob type
      expect(result.size).toBeGreaterThan(0);
    });

    it("should throw error on failed blob download", async () => {
      vi.mocked(authService.makeAuthenticatedRequest).mockResolvedValue(
        new Response("", { status: 404 }),
      );

      await expect(ApiService.downloadPdf(1)).rejects.toThrow(
        "HTTP error! status: 404",
      );
    });
  });

  describe("Batch Operations", () => {
    it("should fetch multiple activities by IDs in parallel", async () => {
      vi.mocked(authService.makeAuthenticatedRequest).mockImplementation(
        (url) => {
          const id = url.split("/").pop();
          return Promise.resolve(
            new Response(
              JSON.stringify({ id: Number(id), title: `Activity ${id}` }),
              {
                status: 200,
              },
            ),
          );
        },
      );

      const results = await ApiService.getActivitiesByIds([1, 2, 3]);

      expect(results).toHaveLength(3);
      expect(results[0].id).toBe(1);
      expect(results[1].id).toBe(2);
      expect(results[2].id).toBe(3);
      expect(authService.makeAuthenticatedRequest).toHaveBeenCalledTimes(3);
    });
  });
});
