import { authService } from "./authService";
import type {
  Activity,
  ActivityMarkdown,
  ActivitiesResponse,
  ResultsData,
  FieldValues,
} from "@/types/activity";
import type {
  UploadPdfDraftResponse,
  UploadPdfDraftOptions,
  ActivityMarkdownsResponse,
  CreateActivityRequest,
  UpdateActivityRequest,
  LessonPlanRequest,
  SearchCriteria,
} from "@/types/api";
import { ApiRequestMixin } from "./apiService";

const EMBEDDED_MARKDOWN_IMAGE_RE =
  /(?:<!--\s*learnhub-image[\s\S]*?-->\s*)?!\[[^\]]*]\(data:[^)\s"']+;base64,[A-Za-z0-9+/=\r\n]+\)/gi;
const EMBEDDED_HTML_IMAGE_RE =
  /<img\b[^>]*\bsrc\s*=\s*(?:"data:[^"]+;base64,[A-Za-z0-9+/=\r\n]+"|'data:[^']+;base64,[A-Za-z0-9+/=\r\n]+'|data:[^\s>]+;base64,[A-Za-z0-9+/=\r\n]+)[^>]*>/gi;
const BASE64_DATA_URI_RE = /data:[^,\s)"']+;base64,[A-Za-z0-9+/=\r\n]+/gi;
const MAX_REGENERATE_IMAGE_CONTEXT_CHARS = 16000;

function sanitizeRegenerateImageContext(context?: string): string {
  if (!context) return "";

  const withoutEmbeddedImages = context
    .replace(EMBEDDED_MARKDOWN_IMAGE_RE, "[Bild]")
    .replace(EMBEDDED_HTML_IMAGE_RE, "[Bild]")
    .replace(BASE64_DATA_URI_RE, "[Bilddaten entfernt]");

  if (withoutEmbeddedImages.length <= MAX_REGENERATE_IMAGE_CONTEXT_CHARS) {
    return withoutEmbeddedImages;
  }

  return `${withoutEmbeddedImages.slice(
    0,
    MAX_REGENERATE_IMAGE_CONTEXT_CHARS,
  )}\n\n[... gekuerzt ...]`;
}

/**
 * Activity-related API methods: CRUD, recommendations, PDF/DOCX downloads,
 * markdown generation, and field values.
 */
export const ActivityApi = {
  /**
   * Get activities with pagination
   */
  async getActivities(params: SearchCriteria = {}) {
    const queryParams = new URLSearchParams();
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== "") {
        if (Array.isArray(value)) {
          value.forEach((v) => queryParams.append(key, String(v)));
        } else {
          queryParams.append(key, String(value));
        }
      }
    });

    return ApiRequestMixin.request<ActivitiesResponse>(
      `/api/activities/?${queryParams.toString()}`,
    );
  },

  /**
   * Get activity by ID
   */
  async getActivity(id: string) {
    return ApiRequestMixin.request<Activity>(`/api/activities/${id}`);
  },

  /**
   * Get stored markdown contents for editing an activity.
   */
  async getActivityMarkdowns(activityId: string) {
    return ApiRequestMixin.request<ActivityMarkdown[]>(
      `/api/activities/${activityId}/markdowns`,
    );
  },

  /**
   * Get multiple activities by IDs
   */
  async getActivitiesByIds(ids: string[]) {
    const promises = ids.map((id) => this.getActivity(id));
    const results = await Promise.all(promises);
    return results;
  },

  /**
   * Get recommendations
   */
  async getRecommendations(params: string): Promise<ResultsData> {
    return ApiRequestMixin.request<ResultsData>(
      `/api/activities/recommendations?${params}`,
    );
  },

  /**
   * Create activity
   */
  async createActivity(data: CreateActivityRequest) {
    return ApiRequestMixin.request("/api/activities/create", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(data),
    });
  },

  /**
   * Delete activity (admin only)
   */
  async deleteActivity(activityId: string) {
    return ApiRequestMixin.request(`/api/activities/${activityId}`, {
      method: "DELETE",
    });
  },

  /**
   * Update activity (admin only)
   */
  async updateActivity(activityId: string, data: UpdateActivityRequest) {
    return ApiRequestMixin.request<Activity>(`/api/activities/${activityId}`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(data),
    });
  },

  /**
   * Generate lesson plan PDF
   */
  async generateLessonPlan(data: LessonPlanRequest) {
    const response = await authService.makeAuthenticatedRequest(
      "/api/activities/lesson-plan",
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(data),
      },
    );

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(
        errorData.message || `HTTP error! status: ${response.status}`,
      );
    }

    return response.blob();
  },

  /**
   * Download a stored document by document ID
   */
  async downloadDocument(documentId: string) {
    const response = await authService.makeAuthenticatedRequest(
      `/api/documents/${documentId}/download`,
    );

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    return response.blob();
  },

  /**
   * Get a stored markdown rendered as PDF by markdown ID
   */
  async getMarkdownPdf(markdownId: string) {
    const response = await authService.makeAuthenticatedRequest(
      `/api/markdowns/${markdownId}/pdf`,
    );

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    return response.blob();
  },

  /**
   * Get a stored markdown rendered as DOCX (Word) by markdown ID
   */
  async getMarkdownDocx(markdownId: string) {
    const response = await authService.makeAuthenticatedRequest(
      `/api/markdowns/${markdownId}/docx`,
    );

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    return response.blob();
  },

  /**
   * Get field values from server
   */
  async getFieldValues() {
    return ApiRequestMixin.request<FieldValues>("/api/meta/field-values");
  },

  /**
   * Get current environment from server
   */
  async getEnvironment() {
    return ApiRequestMixin.request<{ environment: string }>(
      "/api/meta/environment",
    );
  },

  /**
   * Upload PDF for the 2-step activity creation flow.
   */
  async uploadPdfDraft(file: File, options: UploadPdfDraftOptions = {}) {
    const formData = new FormData();
    formData.append("pdf_file", file);
    formData.append("extractMetadata", String(options.extractMetadata ?? true));

    return ApiRequestMixin.request<UploadPdfDraftResponse>(
      "/api/activities/upload-pdf-draft",
      {
        method: "POST",
        body: formData,
      },
    );
  },

  async regenerateMetadata(documentId: string) {
    return ApiRequestMixin.request<UploadPdfDraftResponse>(
      "/api/activities/regenerate-metadata",
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ documentId }),
      },
    );
  },

  /**
   * Re-generate a single exercise image with an optional custom prompt.
   */
  async regenerateImage(params: {
    imageId: string;
    description: string;
    customPrompt?: string;
    exerciseContext?: string;
  }): Promise<string> {
    const data = await ApiRequestMixin.request<{ imageMarkdown: string }>(
      "/api/activities/regenerate-image",
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          imageId: params.imageId,
          description: params.description,
          customPrompt: params.customPrompt ?? "",
          exerciseContext: sanitizeRegenerateImageContext(
            params.exerciseContext,
          ),
        }),
      },
    );
    return data.imageMarkdown;
  },

  /**
   * Generate all activity markdowns (Deckblatt, Artikulationsschema, Hintergrundwissen).
   */
  async generateActivityMarkdowns(
    documentId: string,
    metadata?: Record<string, unknown>,
    types?: string[],
  ) {
    return ApiRequestMixin.request<ActivityMarkdownsResponse>(
      "/api/activities/generate-markdowns",
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          documentId: documentId,
          metadata: metadata,
          types: types,
        }),
      },
    );
  },

  /**
   * Download combined activity PDF
   */
  async downloadActivityPdf(activityId: string) {
    const response = await authService.makeAuthenticatedRequest(
      `/api/activities/${activityId}/pdf`,
    );

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    return response.blob();
  },

  /**
   * Download combined activity DOCX
   */
  async downloadActivityDocx(activityId: string) {
    const response = await authService.makeAuthenticatedRequest(
      `/api/activities/${activityId}/docx`,
    );

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    return response.blob();
  },

  /**
   * Render markdown text to a preview PDF.
   */
  async previewMarkdownPdf(
    markdown: string,
    orientation?: "portrait" | "landscape",
    activityName?: string,
  ) {
    const response = await authService.makeAuthenticatedRequest(
      "/api/markdowns/preview-pdf",
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ markdown, orientation, activityName }),
      },
    );

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(
        (errorData as Record<string, string>).error ||
          `HTTP error! status: ${response.status}`,
      );
    }

    return response.blob();
  },
};
