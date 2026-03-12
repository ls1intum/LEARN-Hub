import React, { useState, useCallback, useRef, useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  FileText,
  CheckCircle,
  Loader2,
  ArrowLeft,
  Save,
  Eye,
  Edit3,
  AlertCircle,
} from "lucide-react";
import { apiService } from "@/services/apiService";
import { ActivityForm } from "@/components/forms/ActivityForm";
import { LoadingState, SkeletonGrid } from "@/components/ui/LoadingState";
import { ErrorDisplay } from "@/components/ui/ErrorDisplay";
import { logger } from "@/services/logger";
import type { Activity } from "@/types/activity";

// ─── Types ───────────────────────────────────────────────────────

interface ActivityFormData {
  name: string;
  description: string;
  source: string;
  age_min: number;
  age_max: number;
  format: string;
  bloom_level: string;
  duration_min_minutes: number;
  duration_max_minutes: number;
  mental_load: string;
  physical_energy: string;
  prep_time_minutes: number;
  cleanup_time_minutes: number;
  resources_needed: string[];
  topics: string[];
  document_id: number | string | null;
  [key: string]: string | number | boolean | string[] | null | undefined;
}

type Step = "metadata" | "artikulationsschema";

// ─── Component ───────────────────────────────────────────────────

export const ActivityEditPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  // Loading / error state for initial fetch
  const [activity, setActivity] = useState<Activity | null>(null);
  const [isLoadingActivity, setIsLoadingActivity] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);

  // Flow state
  const [currentStep, setCurrentStep] = useState<Step>("metadata");

  // Metadata form state (saved when moving to step 2)
  const [savedMetadata, setSavedMetadata] = useState<ActivityFormData | null>(
    null,
  );

  // Artikulationsschema state
  const [artikulationsschemaMarkdown, setArtikulationsschemaMarkdown] =
    useState<string>("");

  // PDF preview state
  const [previewPdfUrl, setPreviewPdfUrl] = useState<string | null>(null);
  const [isRenderingPreview, setIsRenderingPreview] = useState(false);
  const [isPreviewModalOpen, setIsPreviewModalOpen] = useState(false);

  // Save state
  const [isSaving, setIsSaving] = useState(false);
  const [saveError, setSaveError] = useState<string | null>(null);

  // Debounce ref for preview rendering
  const debounceTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  // ─── Load Activity ──────────────────────────────────────────────

  useEffect(() => {
    if (!id) return;
    setIsLoadingActivity(true);
    setLoadError(null);
    apiService
      .getActivity(id)
      .then((data) => {
        setActivity(data);
        setArtikulationsschemaMarkdown(
          data.artikulationsschema_markdown || "",
        );
      })
      .catch((err) => {
        logger.error("Failed to load activity", err, "ActivityEditPage");
        setLoadError(
          err instanceof Error ? err.message : "Failed to load activity",
        );
      })
      .finally(() => setIsLoadingActivity(false));
  }, [id]);

  // Cleanup blob URL on unmount or change
  useEffect(() => {
    return () => {
      if (previewPdfUrl) {
        URL.revokeObjectURL(previewPdfUrl);
      }
    };
  }, [previewPdfUrl]);

  // ─── Metadata Step Handlers ─────────────────────────────────────

  const handleMetadataNext = async (formData: ActivityFormData) => {
    setSavedMetadata(formData);
    setCurrentStep("artikulationsschema");

    // Render preview if markdown already exists
    if (artikulationsschemaMarkdown) {
      debouncedRenderPreview(artikulationsschemaMarkdown);
    }
  };

  // ─── Preview Rendering ──────────────────────────────────────────

  const renderPreview = useCallback(async (markdown: string) => {
    if (!markdown.trim()) {
      setPreviewPdfUrl(null);
      return;
    }

    setIsRenderingPreview(true);
    try {
      const blob = await apiService.previewArtikulationsschemaPdf(markdown);
      const url = URL.createObjectURL(blob);
      setPreviewPdfUrl((prev) => {
        if (prev) URL.revokeObjectURL(prev);
        return url;
      });
    } catch (error) {
      logger.error("Preview render error", error, "ActivityEditPage");
    } finally {
      setIsRenderingPreview(false);
    }
  }, []);

  const debouncedRenderPreview = useCallback(
    (markdown: string) => {
      if (debounceTimer.current) {
        clearTimeout(debounceTimer.current);
      }
      debounceTimer.current = setTimeout(() => {
        renderPreview(markdown);
      }, 800);
    },
    [renderPreview],
  );

  const handleMarkdownChange = (
    e: React.ChangeEvent<HTMLTextAreaElement>,
  ) => {
    const newMarkdown = e.target.value;
    setArtikulationsschemaMarkdown(newMarkdown);
    debouncedRenderPreview(newMarkdown);
  };

  // ─── Save ───────────────────────────────────────────────────────

  const handleSave = async () => {
    if (!savedMetadata || !id) return;

    setIsSaving(true);
    setSaveError(null);
    try {
      await apiService.updateActivity(id, {
        name: savedMetadata.name,
        description: savedMetadata.description,
        source: savedMetadata.source || undefined,
        age_min: savedMetadata.age_min,
        age_max: savedMetadata.age_max,
        format: savedMetadata.format,
        resources_needed: savedMetadata.resources_needed,
        bloom_level: savedMetadata.bloom_level,
        duration_min_minutes: savedMetadata.duration_min_minutes,
        duration_max_minutes: savedMetadata.duration_max_minutes || undefined,
        prep_time_minutes: savedMetadata.prep_time_minutes,
        cleanup_time_minutes: savedMetadata.cleanup_time_minutes,
        mental_load: savedMetadata.mental_load || undefined,
        physical_energy: savedMetadata.physical_energy || undefined,
        topics: savedMetadata.topics,
        artikulationsschema_markdown:
          artikulationsschemaMarkdown || undefined,
      });

      navigate(`/activity-details/${id}`);
    } catch (error) {
      logger.error("Save error", error, "ActivityEditPage");
      setSaveError(
        error instanceof Error ? error.message : "Failed to save activity",
      );
    } finally {
      setIsSaving(false);
    }
  };

  // ─── Step Indicator ─────────────────────────────────────────────

  const steps = [
    { key: "metadata" as Step, label: "Edit Metadata" },
    {
      key: "artikulationsschema" as Step,
      label: "Artikulationsschema",
    },
  ];

  const currentStepIndex = steps.findIndex((s) => s.key === currentStep);

  // ─── Render ─────────────────────────────────────────────────────

  if (isLoadingActivity) {
    return (
      <div className="w-full">
        <LoadingState isLoading={true} fallback={<SkeletonGrid />}>
          <div className="text-center py-12">
            <p className="text-muted-foreground">Loading activity...</p>
          </div>
        </LoadingState>
      </div>
    );
  }

  if (loadError || !activity) {
    return (
      <div className="w-full">
        <div className="text-center py-12">
          <ErrorDisplay
            error={loadError || "Activity not found"}
            onRetry={() => {
              if (id) {
                setIsLoadingActivity(true);
                setLoadError(null);
                apiService
                  .getActivity(id)
                  .then((data) => {
                    setActivity(data);
                    setArtikulationsschemaMarkdown(
                      data.artikulationsschema_markdown || "",
                    );
                  })
                  .catch((err) =>
                    setLoadError(
                      err instanceof Error
                        ? err.message
                        : "Failed to load activity",
                    ),
                  )
                  .finally(() => setIsLoadingActivity(false));
              }
            }}
          />
          <div className="mt-4">
            <Button onClick={() => navigate(-1)}>Go Back</Button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="w-full px-4 py-6">
      {/* Step Indicator */}
      <div className="mb-8">
        <div className="flex items-center justify-center gap-2">
          {steps.map((step, idx) => (
            <React.Fragment key={step.key}>
              <div className="flex items-center gap-2">
                <div
                  className={`flex items-center justify-center w-8 h-8 rounded-full text-sm font-medium ${
                    idx < currentStepIndex
                      ? "bg-primary text-primary-foreground"
                      : idx === currentStepIndex
                        ? "bg-primary text-primary-foreground ring-2 ring-primary/30"
                        : "bg-muted text-muted-foreground"
                  }`}
                >
                  {idx < currentStepIndex ? (
                    <CheckCircle className="h-4 w-4" />
                  ) : (
                    idx + 1
                  )}
                </div>
                <span
                  className={`text-sm font-medium ${
                    idx === currentStepIndex
                      ? "text-foreground"
                      : "text-muted-foreground"
                  }`}
                >
                  {step.label}
                </span>
              </div>
              {idx < steps.length - 1 && (
                <div
                  className={`w-12 h-0.5 ${
                    idx < currentStepIndex ? "bg-primary" : "bg-muted"
                  }`}
                />
              )}
            </React.Fragment>
          ))}
        </div>
      </div>

      {/* Step: Metadata */}
      {currentStep === "metadata" && (
        <div className="max-w-2xl mx-auto">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Edit3 className="h-5 w-5" />
                Edit Activity Metadata
              </CardTitle>
              <CardDescription>
                Update the metadata for "{activity.name}".
              </CardDescription>
            </CardHeader>
            <CardContent>
              <ActivityForm
                initialData={
                  savedMetadata ||
                  ({
                    name: activity.name,
                    description: activity.description,
                    source: activity.source || "",
                    age_min: activity.age_min,
                    age_max: activity.age_max,
                    format: activity.format,
                    bloom_level: activity.bloom_level,
                    duration_min_minutes: activity.duration_min_minutes,
                    duration_max_minutes:
                      activity.duration_max_minutes ||
                      activity.duration_min_minutes,
                    mental_load: activity.mental_load || "medium",
                    physical_energy: activity.physical_energy || "medium",
                    prep_time_minutes: activity.prep_time_minutes || 5,
                    cleanup_time_minutes: activity.cleanup_time_minutes || 5,
                    resources_needed: activity.resources_needed || [],
                    topics: activity.topics || [],
                    document_id: activity.document_id || null,
                  } as Partial<ActivityFormData>)
                }
                onSubmit={handleMetadataNext}
                onCancel={() => navigate(`/activity-details/${id}`)}
                isLoading={false}
                submitLabel="Next: Artikulationsschema"
                cancelLabel="Cancel"
              />
            </CardContent>
          </Card>
        </div>
      )}

      {/* Step: Artikulationsschema */}
      {currentStep === "artikulationsschema" && (
        <div className="space-y-4">
          {/* Back / Save header */}
          <div className="flex items-center justify-between">
            <Button
              variant="outline"
              onClick={() => setCurrentStep("metadata")}
            >
              <ArrowLeft className="h-4 w-4 mr-2" />
              Back to Metadata
            </Button>
            <Button onClick={handleSave} disabled={isSaving}>
              {isSaving ? (
                <>
                  <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                  Saving...
                </>
              ) : (
                <>
                  <Save className="h-4 w-4 mr-2" />
                  Save Changes
                </>
              )}
            </Button>
          </div>

          {saveError && (
            <div className="flex items-center gap-2 p-3 bg-destructive/10 border border-destructive/20 rounded-lg">
              <AlertCircle className="h-4 w-4 text-destructive" />
              <p className="text-sm text-destructive">{saveError}</p>
            </div>
          )}

          <div className="grid grid-cols-1 lg:grid-cols-2 gap-4 h-[calc(100vh-16rem)]">
            {/* Left: Markdown Editor */}
            <Card className="flex flex-col min-h-0">
              <CardHeader className="pb-2 flex-shrink-0">
                <CardTitle className="flex items-center gap-2 text-base">
                  <Edit3 className="h-4 w-4" />
                  Markdown Editor
                </CardTitle>
              </CardHeader>
              <CardContent className="flex-1 min-h-0 pb-4">
                <textarea
                  value={artikulationsschemaMarkdown}
                  onChange={handleMarkdownChange}
                  className="w-full h-full min-h-[400px] resize-none rounded-md border border-input bg-background px-3 py-2 text-sm font-mono ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                  placeholder="# Artikulationsschema&#10;&#10;**Thema:** ...&#10;&#10;| Zeit | Phase | Handlungsschritte | Sozialform | Kompetenzen | Medien/Material |&#10;|------|-------|-------------------|------------|-------------|-----------------|&#10;| 5 min | Einstieg | ... | Plenum | ... | ... |"
                />
              </CardContent>
            </Card>

            {/* Right: PDF Preview (desktop only) */}
            <Card className="hidden lg:flex flex-col min-h-0">
              <CardHeader className="pb-2 flex-shrink-0">
                <CardTitle className="flex items-center gap-2 text-base">
                  <Eye className="h-4 w-4" />
                  PDF Preview
                  {isRenderingPreview && (
                    <Loader2 className="h-3 w-3 animate-spin text-muted-foreground" />
                  )}
                </CardTitle>
              </CardHeader>
              <CardContent className="flex-1 min-h-0 pb-4">
                {previewPdfUrl ? (
                  <iframe
                    src={previewPdfUrl}
                    className="w-full h-full min-h-[400px] rounded-md border"
                    title="Artikulationsschema Preview"
                  />
                ) : (
                  <div className="w-full h-full min-h-[400px] flex items-center justify-center rounded-md border bg-muted/30">
                    <div className="text-center text-muted-foreground">
                      <FileText className="h-12 w-12 mx-auto mb-2 opacity-50" />
                      <p className="text-sm">
                        {isRenderingPreview
                          ? "Rendering preview..."
                          : "Edit the markdown to see a PDF preview"}
                      </p>
                    </div>
                  </div>
                )}
              </CardContent>
            </Card>
          </div>

          {/* Mobile: Render Preview button */}
          <div className="lg:hidden mt-4">
            <Button
              variant="outline"
              className="w-full"
              onClick={() => {
                if (!previewPdfUrl && artikulationsschemaMarkdown) {
                  debouncedRenderPreview(artikulationsschemaMarkdown);
                }
                setIsPreviewModalOpen(true);
              }}
              disabled={!artikulationsschemaMarkdown}
            >
              <Eye className="h-4 w-4 mr-2" />
              Render Preview
            </Button>
          </div>

          {/* Mobile: PDF Preview Dialog */}
          <Dialog
            open={isPreviewModalOpen}
            onOpenChange={setIsPreviewModalOpen}
          >
            <DialogContent className="max-w-[95vw] w-full h-[85vh] flex flex-col p-0">
              <DialogHeader className="px-6 pt-6 pb-2 flex-shrink-0">
                <DialogTitle className="flex items-center gap-2">
                  <Eye className="h-4 w-4" />
                  PDF Preview
                  {isRenderingPreview && (
                    <Loader2 className="h-3 w-3 animate-spin text-muted-foreground" />
                  )}
                </DialogTitle>
              </DialogHeader>
              <div className="flex-1 min-h-0 px-6 pb-6">
                {previewPdfUrl ? (
                  <iframe
                    src={previewPdfUrl}
                    className="w-full h-full rounded-md border"
                    title="Artikulationsschema Preview"
                  />
                ) : (
                  <div className="w-full h-full flex items-center justify-center rounded-md border bg-muted/30">
                    <div className="text-center text-muted-foreground">
                      <FileText className="h-12 w-12 mx-auto mb-2 opacity-50" />
                      <p className="text-sm">
                        {isRenderingPreview
                          ? "Rendering preview..."
                          : "Edit the markdown to see a PDF preview"}
                      </p>
                    </div>
                  </div>
                )}
              </div>
            </DialogContent>
          </Dialog>
        </div>
      )}
    </div>
  );
};
