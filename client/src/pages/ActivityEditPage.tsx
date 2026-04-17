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
import {
  Save,
  Edit3,
  AlertCircle,
  Loader2,
  RefreshCw,
  Sparkles,
} from "lucide-react";
import { apiService } from "@/services/apiService";
import {
  ActivityForm,
  type ActivityFormData,
} from "@/components/forms/ActivityForm";
import { LoadingState, SkeletonGrid } from "@/components/ui/LoadingState";
import { ErrorDisplay } from "@/components/ui/ErrorDisplay";
import { StepIndicator } from "@/components/ui/StepIndicator";
import { PageHeader } from "@/components/ui/PageHeader";
import {
  MarkdownEditorWithPreview,
  type RegenerateImageParams,
} from "@/components/ui/MarkdownEditorWithPreview";
import { logger } from "@/services/logger";
import type { Activity } from "@/types/activity";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { useTranslation } from "react-i18next";
import { getAppScrollTop } from "@/utils/scroll";

type Step = "metadata" | "documents";
type MarkdownTab =
  | "deckblatt"
  | "artikulationsschema"
  | "hintergrundwissen"
  | "uebung"
  | "uebung_loesung";

const MARKDOWN_TAB_KEYS: MarkdownTab[] = [
  "deckblatt",
  "artikulationsschema",
  "hintergrundwissen",
  "uebung",
  "uebung_loesung",
];

// ─── Component ───────────────────────────────────────────────────

export const ActivityEditPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { t } = useTranslation();

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
  const [uebungMarkdown, setUebungMarkdown] = useState<string>("");
  const [uebungLoesungMarkdown, setUebungLoesungMarkdown] =
    useState<string>("");
  const [activeMarkdownTab, setActiveMarkdownTab] =
    useState<MarkdownTab>("deckblatt");

  // Save state
  const [isSaving, setIsSaving] = useState(false);
  const [saveError, setSaveError] = useState<string | null>(null);

  // Generation state
  const [isGenerating, setIsGenerating] = useState(false);
  const [generateError, setGenerateError] = useState<string | null>(null);
  const [isRegeneratingMetadata, setIsRegeneratingMetadata] = useState(false);

  // Document ID for AI regeneration (from persisted source_pdf)
  const documentId =
    activity?.documents?.find((d) => d.type === "source_pdf")?.id || null;

  const handleRegenerateImage = useCallback(
    async (params: RegenerateImageParams) => {
      return apiService.regenerateImage({
        imageId: params.imageId,
        description: params.description,
        customPrompt: params.customPrompt,
        exerciseContext: [uebungMarkdown, uebungLoesungMarkdown]
          .filter(Boolean)
          .join("\n\n"),
      });
    },
    [uebungMarkdown, uebungLoesungMarkdown],
  );

  /** Whether the currently active markdown tab already has content */
  const activeTabHasContent =
    (activeMarkdownTab === "deckblatt" && !!deckblattMarkdown) ||
    (activeMarkdownTab === "artikulationsschema" &&
      !!artikulationsschemaMarkdown) ||
    (activeMarkdownTab === "hintergrundwissen" &&
      !!hintergrundwissenMarkdown) ||
    (activeMarkdownTab === "uebung" && !!uebungMarkdown) ||
    (activeMarkdownTab === "uebung_loesung" && !!uebungLoesungMarkdown);

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
        setUebungMarkdown(
          data.markdowns?.find((m) => m.type === "uebung")?.content || "",
        );
        setUebungLoesungMarkdown(
          data.markdowns?.find((m) => m.type === "uebung_loesung")?.content ||
            "",
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

  // ─── Metadata Step Handlers ─────────────────────────────────────

  const handleMetadataNext = async (formData: ActivityFormData) => {
    setSavedMetadata(formData);
    setCurrentStep("documents");
  };

  // ─── AI Regeneration Handlers ───────────────────────────────────

  const handleRegenerateMetadata = async () => {
    if (!documentId) return;

    setIsRegeneratingMetadata(true);
    setSaveError(null);
    try {
      const result = await apiService.regenerateMetadata(documentId);
      if (result.extractedData) {
        setSavedMetadata(
          (prev) =>
            ({
              ...prev,
              ...(result.extractedData as Partial<ActivityFormData>),
              documentId: documentId,
            }) as ActivityFormData,
        );
      }
    } catch (error) {
      logger.error("Metadata regeneration error", error, "ActivityEditPage");
      setSaveError(
        error instanceof Error
          ? error.message
          : "Failed to regenerate metadata",
      );
    } finally {
      setIsRegeneratingMetadata(false);
    }
  };

  const generateActiveMarkdown = async () => {
    if (!documentId) return;

    setIsGenerating(true);
    setGenerateError(null);
    // uebung and uebung_loesung are always generated together in one call
    const typesToRequest =
      activeMarkdownTab === "uebung" || activeMarkdownTab === "uebung_loesung"
        ? ["uebung", "uebung_loesung"]
        : [activeMarkdownTab];
    try {
      const result = await apiService.generateActivityMarkdowns(
        documentId,
        savedMetadata as unknown as Record<string, unknown> | undefined,
        typesToRequest,
      );
      if (result.deckblattMarkdown) {
        setDeckblattMarkdown(result.deckblattMarkdown);
      }
      if (result.artikulationsschemaMarkdown) {
        setArtikulationsschemaMarkdown(result.artikulationsschemaMarkdown);
      }
      if (result.hintergrundwissenMarkdown) {
        setHintergrundwissenMarkdown(result.hintergrundwissenMarkdown);
      }
      if (result.uebungMarkdown) {
        setUebungMarkdown(result.uebungMarkdown);
      }
      if (result.uebungLoesungMarkdown) {
        setUebungLoesungMarkdown(result.uebungLoesungMarkdown);
      }
    } catch (error) {
      logger.error("Markdown generation error", error, "ActivityEditPage");
      setGenerateError(
        error instanceof Error ? error.message : "Failed to generate document",
      );
    } finally {
      setIsGenerating(false);
    }
  };

  // ─── Preview Rendering ──────────────────────────────────────────

  const renderPreviewLandscape = useCallback(
    (markdown: string) =>
      apiService.previewMarkdownPdf(
        markdown,
        "landscape",
        savedMetadata?.name || activity?.name || "",
      ),
    [activity?.name, savedMetadata?.name],
  );

  const renderPreviewPortrait = useCallback(
    (markdown: string) =>
      apiService.previewMarkdownPdf(
        markdown,
        "portrait",
        savedMetadata?.name || activity?.name || "",
      ),
    [activity?.name, savedMetadata?.name],
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
        uebungMarkdown: uebungMarkdown || undefined,
        uebungLoesungMarkdown: uebungLoesungMarkdown || undefined,
      });

      navigate(`/activity-details/${id}`, {
        state: {
          backTo: `/activity-edit/${id}`,
          restoreScrollY: getAppScrollTop(),
        },
      });
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
    { key: "metadata" as Step, label: t("editActivity.editMetadata") },
    {
      key: "documents" as Step,
      label: t("upload.stepDocuments"),
    },
  ];

  const currentStepIndex = steps.findIndex((s) => s.key === currentStep);

  // ─── Render ─────────────────────────────────────────────────────

  if (isLoadingActivity) {
    return (
      <div className="w-full">
        <LoadingState isLoading={true} fallback={<SkeletonGrid />}>
          <div className="text-center py-12">
            <p className="text-muted-foreground">
              {t("editActivity.loadingActivity")}
            </p>
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
            error={loadError || t("editActivity.activityNotFound")}
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
                    setUebungMarkdown(
                      data.markdowns?.find((m) => m.type === "uebung")
                        ?.content || "",
                    );
                    setUebungLoesungMarkdown(
                      data.markdowns?.find((m) => m.type === "uebung_loesung")
                        ?.content || "",
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
            <Button onClick={() => navigate(-1)}>
              {t("editActivity.goBack")}
            </Button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="py-6">
      {/* Page Header & Step Indicator */}
      <div className="space-y-6 mb-8">
        <PageHeader
          title={t("editActivity.title")}
          description={t("editActivity.editingName", { name: activity.name })}
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
                  label: t("editActivity.nextDocuments"),
                  variant: "outline",
                  size: "icon",
                  ariaLabel: t("editActivity.nextDocuments"),
                  className: "h-9 w-9",
                  formId: "activity-edit-form",
                }
              : currentStep === "documents"
                ? {
                    label: t("editActivity.saveChanges"),
                    variant: "default",
                    onClick: handleSave,
                    icon: <Save className="h-4 w-4" />,
                    disabled: isSaving || isGenerating,
                    loading: isSaving,
                    loadingLabel: t("upload.saving"),
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
              <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
                <div>
                  <CardTitle className="flex items-center gap-2">
                    <Edit3 className="h-5 w-5" />
                    {t("editActivity.editActivityMetadata")}
                  </CardTitle>
                  <CardDescription>
                    {t("editActivity.updateMetadata", { name: activity.name })}
                  </CardDescription>
                </div>
                {documentId && (
                  <Button
                    type="button"
                    variant="outline"
                    className="gap-2"
                    onClick={handleRegenerateMetadata}
                    disabled={!documentId || isRegeneratingMetadata}
                  >
                    {isRegeneratingMetadata ? (
                      <>
                        <Loader2 className="h-4 w-4 animate-spin" />
                        {t("upload.regenerating")}
                      </>
                    ) : (
                      <>
                        <RefreshCw className="h-4 w-4" />
                        {t("upload.rerunAi")}
                      </>
                    )}
                  </Button>
                )}
              </div>
            </CardHeader>
            <CardContent>
              {saveError && currentStep === "metadata" && (
                <div className="flex items-center gap-2 p-3 mb-4 bg-destructive/10 border border-destructive/20 rounded-lg">
                  <AlertCircle className="h-4 w-4 text-destructive" />
                  <p className="text-sm text-destructive">{saveError}</p>
                </div>
              )}
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
              <div className="flex grow items-center gap-3">
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
                    {MARKDOWN_TAB_KEYS.map((key) => (
                      <SelectItem key={key} value={key}>
                        {t(`upload.${key}`)}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              {documentId && (
                <Button
                  type="button"
                  variant="outline"
                  className="gap-2"
                  onClick={() => void generateActiveMarkdown()}
                  disabled={!documentId || isGenerating}
                >
                  {isGenerating ? (
                    <>
                      <Loader2 className="h-4 w-4 animate-spin" />
                      {t("upload.generating")}
                    </>
                  ) : (
                    <>
                      {activeTabHasContent ? (
                        <RefreshCw className="h-4 w-4" />
                      ) : (
                        <Sparkles className="h-4 w-4" />
                      )}
                      {activeTabHasContent
                        ? t("upload.regenerate", {
                            doc: t(`upload.${activeMarkdownTab}`),
                          })
                        : t("upload.generate", {
                            doc: t(`upload.${activeMarkdownTab}`),
                          })}
                    </>
                  )}
                </Button>
              )}
            </CardContent>
          </Card>

          {isGenerating ? (
            <Card>
              <CardContent className="flex flex-col items-center justify-center py-16">
                <Loader2 className="h-8 w-8 animate-spin text-primary mb-4" />
                <p className="text-lg font-medium">
                  {t("upload.generatingDoc", {
                    doc: t(`upload.${activeMarkdownTab}`),
                  })}
                </p>
                <p className="text-sm text-muted-foreground mt-1">
                  {t("upload.generatingDocDesc")}
                </p>
              </CardContent>
            </Card>
          ) : generateError ? (
            <Card>
              <CardContent className="py-8">
                <div className="flex flex-col items-center gap-4">
                  <AlertCircle className="h-8 w-8 text-destructive" />
                  <p className="text-destructive font-medium">
                    {generateError}
                  </p>
                  <Button
                    variant="outline"
                    onClick={() => {
                      void generateActiveMarkdown();
                    }}
                  >
                    {t("upload.retryGeneration")}
                  </Button>
                </div>
              </CardContent>
            </Card>
          ) : (
            <>
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
              {activeMarkdownTab === "uebung" && (
                <MarkdownEditorWithPreview
                  value={uebungMarkdown}
                  onChange={setUebungMarkdown}
                  renderPreviewFn={renderPreviewPortrait}
                  onRegenerateImage={handleRegenerateImage}
                />
              )}
              {activeMarkdownTab === "uebung_loesung" && (
                <MarkdownEditorWithPreview
                  value={uebungLoesungMarkdown}
                  onChange={setUebungLoesungMarkdown}
                  renderPreviewFn={renderPreviewPortrait}
                  onRegenerateImage={handleRegenerateImage}
                />
              )}
            </>
          )}
        </div>
      )}
    </div>
  );
};
