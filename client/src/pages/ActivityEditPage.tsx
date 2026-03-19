import React, { useState, useCallback, useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Save, Edit3, AlertCircle } from "lucide-react";
import { apiService } from "@/services/apiService";
import {
  ActivityForm,
  type ActivityFormData,
} from "@/components/forms/ActivityForm";
import { LoadingState, SkeletonGrid } from "@/components/ui/LoadingState";
import { ErrorDisplay } from "@/components/ui/ErrorDisplay";
import { StepIndicator } from "@/components/ui/StepIndicator";
import { PageHeader } from "@/components/ui/PageHeader";
import { MarkdownEditorWithPreview } from "@/components/ui/MarkdownEditorWithPreview";
import { logger } from "@/services/logger";
import type { Activity } from "@/types/activity";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

type Step = "metadata" | "documents";
type MarkdownTab = "deckblatt" | "artikulationsschema" | "hintergrundwissen";

const MARKDOWN_TAB_LABELS: Record<MarkdownTab, string> = {
  deckblatt: "Deckblatt",
  artikulationsschema: "Artikulationsschema",
  hintergrundwissen: "Hintergrundwissen",
};

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
  const [deckblattMarkdown, setDeckblattMarkdown] = useState<string>("");
  const [hintergrundwissenMarkdown, setHintergrundwissenMarkdown] =
    useState<string>("");
  const [activeMarkdownTab, setActiveMarkdownTab] =
    useState<MarkdownTab>("deckblatt");

  // Save state
  const [isSaving, setIsSaving] = useState(false);
  const [saveError, setSaveError] = useState<string | null>(null);

  // ─── Load Activity ──────────────────────────────────────────────

  useEffect(() => {
    if (!id) return;
    setIsLoadingActivity(true);
    setLoadError(null);
    apiService
      .getActivity(id)
      .then((data) => {
        setActivity(data);
        const artikulationsMd =
          data.markdowns?.find((m) => m.type === "artikulationsschema")
            ?.content || "";
        setArtikulationsschemaMarkdown(artikulationsMd);
        const deckblattMd =
          data.markdowns?.find((m) => m.type === "deckblatt")?.content || "";
        setDeckblattMarkdown(deckblattMd);
        const hintergrundwissenMd =
          data.markdowns?.find((m) => m.type === "hintergrundwissen")
            ?.content || "";
        setHintergrundwissenMarkdown(hintergrundwissenMd);
      })
      .catch((err) => {
        logger.error("Failed to load activity", err, "ActivityEditPage");
        setLoadError(
          err instanceof Error ? err.message : "Failed to load activity",
        );
      })
      .finally(() => setIsLoadingActivity(false));
  }, [id]);

  // ─── Metadata Step Handlers ─────────────────────────────────────

  const handleMetadataNext = async (formData: ActivityFormData) => {
    setSavedMetadata(formData);
    setCurrentStep("documents");
  };

  // ─── Preview Rendering ──────────────────────────────────────────

  const renderPreviewLandscape = useCallback(
    (markdown: string) => apiService.previewMarkdownPdf(markdown, "landscape"),
    [],
  );

  const renderPreviewPortrait = useCallback(
    (markdown: string) => apiService.previewMarkdownPdf(markdown, "portrait"),
    [],
  );

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
        deckblattMarkdown: deckblattMarkdown || undefined,
        hintergrundwissenMarkdown: hintergrundwissenMarkdown || undefined,
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
      key: "documents" as Step,
      label: "Documents",
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
                      data.markdowns?.find(
                        (m) => m.type === "artikulationsschema",
                      )?.content || "",
                    );
                    setDeckblattMarkdown(
                      data.markdowns?.find((m) => m.type === "deckblatt")
                        ?.content || "",
                    );
                    setHintergrundwissenMarkdown(
                      data.markdowns?.find(
                        (m) => m.type === "hintergrundwissen",
                      )?.content || "",
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
              : currentStep === "documents"
                ? () => setCurrentStep("metadata")
                : undefined
          }
          onForward={
            currentStep === "metadata"
              ? {
                  label: "Next: Documents",
                  variant: "outline",
                  size: "icon",
                  ariaLabel: "Next step",
                  className: "h-9 w-9",
                  formId: "activity-edit-form",
                }
              : currentStep === "documents"
                ? {
                    label: "Save Changes",
                    variant: "default",
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
                    documentId:
                      activity.documents?.find((d) => d.type === "source_pdf")
                        ?.id || null,
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

      {/* Step: Documents (Deckblatt, Artikulationsschema, Hintergrundwissen) */}
      {currentStep === "documents" && (
        <div className="space-y-4 lg:relative lg:left-1/2 lg:w-[calc(100vw-16rem-4rem)] lg:max-w-none lg:-translate-x-1/2">
          {saveError && (
            <div className="flex items-center gap-2 p-3 bg-destructive/10 border border-destructive/20 rounded-lg">
              <AlertCircle className="h-4 w-4 text-destructive" />
              <p className="text-sm text-destructive">{saveError}</p>
            </div>
          )}

          <Card>
            <CardContent className="flex flex-col gap-3 py-4 sm:flex-row sm:items-center sm:justify-between">
              <Select
                value={activeMarkdownTab}
                onValueChange={(value) =>
                  setActiveMarkdownTab(value as MarkdownTab)
                }
              >
                <SelectTrigger className="w-full">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {(
                    Object.entries(MARKDOWN_TAB_LABELS) as [
                      MarkdownTab,
                      string,
                    ][]
                  ).map(([key, label]) => (
                    <SelectItem key={key} value={key}>
                      {label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </CardContent>
          </Card>

          {activeMarkdownTab === "deckblatt" && (
            <MarkdownEditorWithPreview
              value={deckblattMarkdown}
              onChange={setDeckblattMarkdown}
              renderPreviewFn={renderPreviewPortrait}
            />
          )}
          {activeMarkdownTab === "artikulationsschema" && (
            <MarkdownEditorWithPreview
              value={artikulationsschemaMarkdown}
              onChange={setArtikulationsschemaMarkdown}
              renderPreviewFn={renderPreviewLandscape}
            />
          )}
          {activeMarkdownTab === "hintergrundwissen" && (
            <MarkdownEditorWithPreview
              value={hintergrundwissenMarkdown}
              onChange={setHintergrundwissenMarkdown}
              renderPreviewFn={renderPreviewPortrait}
            />
          )}
        </div>
      )}
    </div>
  );
};
