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
  Loader2,
  Save,
  Eye,
  Edit3,
  AlertCircle,
} from "lucide-react";
import { apiService } from "@/services/apiService";
import { ActivityForm } from "@/components/forms/ActivityForm";
import { LoadingState, SkeletonGrid } from "@/components/ui/LoadingState";
import { ErrorDisplay } from "@/components/ui/ErrorDisplay";
import { StepIndicator } from "@/components/ui/StepIndicator";
import { PageHeader } from "@/components/ui/PageHeader";
import { logger } from "@/services/logger";
import type { Activity } from "@/types/activity";

// ─── Types ───────────────────────────────────────────────────────

interface ActivityFormData {
  name: string;
  description: string;
  source: string;
  ageMin: number;
  ageMax: number;
  format: string;
  bloomLevel: string;
  durationMinMinutes: number;
  durationMaxMinutes: number;
  mentalLoad: string;
  physicalEnergy: string;
  prepTimeMinutes: number;
  cleanupTimeMinutes: number;
  resourcesNeeded: string[];
  topics: string[];
  documentId: number | string | null;
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
        setArtikulationsschemaMarkdown(data.artikulationsschemaMarkdown || "");
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

  const handleMarkdownChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
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
        ageMin: savedMetadata.ageMin,
        ageMax: savedMetadata.ageMax,
        format: savedMetadata.format,
        resourcesNeeded: savedMetadata.resourcesNeeded,
        bloomLevel: savedMetadata.bloomLevel,
        durationMinMinutes: savedMetadata.durationMinMinutes,
        durationMaxMinutes: savedMetadata.durationMaxMinutes || undefined,
        prepTimeMinutes: savedMetadata.prepTimeMinutes,
        cleanupTimeMinutes: savedMetadata.cleanupTimeMinutes,
        mentalLoad: savedMetadata.mentalLoad || undefined,
        physicalEnergy: savedMetadata.physicalEnergy || undefined,
        topics: savedMetadata.topics,
        artikulationsschemaMarkdown: artikulationsschemaMarkdown || undefined,
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
                      data.artikulationsschemaMarkdown || "",
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
    <div className="w-full py-6">
      {/* Page Header & Step Indicator */}
      <div className="space-y-6 mb-8">
        <PageHeader
          title="Edit Activity"
          description={`Editing "${activity.name}"`}
        />
        <StepIndicator
          steps={steps}
          currentStepIndex={currentStepIndex}
          onBack={
            currentStep === "metadata"
              ? () => navigate(`/activity-details/${id}`)
              : currentStep === "artikulationsschema"
                ? () => setCurrentStep("metadata")
                : undefined
          }
          onForward={
            currentStep === "metadata"
              ? {
                  label: "Next: Artikulationsschema",
                  formId: "activity-edit-form",
                }
              : currentStep === "artikulationsschema"
                ? {
                    label: "Save Changes",
                    onClick: handleSave,
                    icon: <Save className="h-4 w-4" />,
                    disabled: isSaving,
                    loading: isSaving,
                    loadingLabel: "Saving...",
                  }
                : undefined
          }
        />
      </div>

      {/* Step: Metadata */}
      {currentStep === "metadata" && (
        <div>
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
                    ageMin: activity.ageMin,
                    ageMax: activity.ageMax,
                    format: activity.format,
                    bloomLevel: activity.bloomLevel,
                    durationMinMinutes: activity.durationMinMinutes,
                    durationMaxMinutes:
                      activity.durationMaxMinutes ||
                      activity.durationMinMinutes,
                    mentalLoad: activity.mentalLoad || "medium",
                    physicalEnergy: activity.physicalEnergy || "medium",
                    prepTimeMinutes: activity.prepTimeMinutes || 5,
                    cleanupTimeMinutes: activity.cleanupTimeMinutes || 5,
                    resourcesNeeded: activity.resourcesNeeded || [],
                    topics: activity.topics || [],
                    documentId: activity.documentId || null,
                  } as Partial<ActivityFormData>)
                }
                onSubmit={handleMetadataNext}
                onCancel={() => navigate(`/activity-details/${id}`)}
                isLoading={false}
                hideButtons
                formId="activity-edit-form"
              />
            </CardContent>
          </Card>
        </div>
      )}

      {/* Step: Artikulationsschema */}
      {currentStep === "artikulationsschema" && (
        <div className="space-y-4">

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
