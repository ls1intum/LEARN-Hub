import React, { useState, useCallback, useEffect } from "react";
import { useParams, useNavigate, useLocation } from "react-router-dom";
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
import { Breadcrumb, type BreadcrumbItem } from "@/components/ui/Breadcrumb";
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
import {
  getActivityBackTarget,
  type ActivityNavigationState,
} from "@/utils/activityNavigation";
import type { UpdateActivityRequest } from "@/types/api";

type Step = "metadata" | "documents";
type MarkdownTab =
  | "cover_sheet"
  | "lesson_plan"
  | "background_knowledge"
  | "board_image"
  | "exercise"
  | "exercise_solution";

const MARKDOWN_TAB_KEYS: MarkdownTab[] = [
  "cover_sheet",
  "lesson_plan",
  "background_knowledge",
  "board_image",
  "exercise",
  "exercise_solution",
];

const EMPTY_MARKDOWN_CONTENT: Record<MarkdownTab, string> = {
  cover_sheet: "",
  lesson_plan: "",
  background_knowledge: "",
  board_image: "",
  exercise: "",
  exercise_solution: "",
};

const getEmptyMarkdownContent = () => ({ ...EMPTY_MARKDOWN_CONTENT });

const getMarkdownContentByTab = (
  markdowns: Activity["markdowns"] | undefined,
) => {
  const content = getEmptyMarkdownContent();
  markdowns?.forEach((markdown) => {
    const type = markdown.type as MarkdownTab;
    if (MARKDOWN_TAB_KEYS.includes(type)) {
      content[type] = markdown.content ?? "";
    }
  });
  return content;
};

// ─── Component ───────────────────────────────────────────────────

export const ActivityEditPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const location = useLocation();
  const navigate = useNavigate();
  const { t } = useTranslation();
  const navigationState = location.state as ActivityNavigationState | null;
  const backTo = getActivityBackTarget(navigationState?.backTo);
  const detailPath =
    navigationState?.detailPath ?? (id ? `/library/${id}` : undefined);
  const detailNavigationState: ActivityNavigationState = {
    backTo,
    restoreScrollY: navigationState?.restoreScrollY,
  };
  const handleBack = useCallback(() => {
    if (window.history.length > 1) {
      navigate(-1);
      return;
    }

    if (backTo) {
      navigate(backTo, {
        replace: true,
        state:
          typeof navigationState?.restoreScrollY === "number"
            ? { restoreScrollY: navigationState.restoreScrollY }
            : undefined,
      });
      return;
    }

    if (detailPath) {
      navigate(detailPath, {
        replace: true,
        state: detailNavigationState,
      });
      return;
    }

    navigate("/drafts", { replace: true });
  }, [backTo, detailNavigationState, detailPath, navigate, navigationState]);

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

  // Markdown state
  const [lessonPlanMarkdown, setLessonPlanMarkdown] = useState<string>("");
  const [coverSheetMarkdown, setCoverSheetMarkdown] = useState<string>("");
  const [backgroundKnowledgeMarkdown, setBackgroundKnowledgeMarkdown] =
    useState<string>("");
  const [boardImageMarkdown, setBoardImageMarkdown] = useState<string>("");
  const [exerciseMarkdown, setExerciseMarkdown] = useState<string>("");
  const [exerciseSolutionMarkdown, setExerciseSolutionMarkdown] =
    useState<string>("");
  const [initialMarkdownContent, setInitialMarkdownContent] = useState<
    Record<MarkdownTab, string>
  >(getEmptyMarkdownContent);
  const [activeMarkdownTab, setActiveMarkdownTab] =
    useState<MarkdownTab>("cover_sheet");

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
        exerciseContext: [exerciseMarkdown, exerciseSolutionMarkdown]
          .filter(Boolean)
          .join("\n\n"),
      });
    },
    [exerciseMarkdown, exerciseSolutionMarkdown],
  );

  const handleRegenerateImageForTafelbild = useCallback(
    async (params: RegenerateImageParams) => {
      return apiService.regenerateImage({
        imageId: params.imageId,
        description: params.description,
        customPrompt: params.customPrompt,
        exerciseContext: lessonPlanMarkdown,
        markdownType: "board_image",
      });
    },
    [lessonPlanMarkdown],
  );

  /** Whether the currently active markdown tab already has content */
  const activeTabHasContent =
    (activeMarkdownTab === "cover_sheet" && !!coverSheetMarkdown) ||
    (activeMarkdownTab === "lesson_plan" && !!lessonPlanMarkdown) ||
    (activeMarkdownTab === "background_knowledge" &&
      !!backgroundKnowledgeMarkdown) ||
    (activeMarkdownTab === "board_image" && !!boardImageMarkdown) ||
    (activeMarkdownTab === "exercise" && !!exerciseMarkdown) ||
    (activeMarkdownTab === "exercise_solution" && !!exerciseSolutionMarkdown);

  // ─── Load Activity ──────────────────────────────────────────────

  const applyMarkdownContent = useCallback(
    (markdownContent: Record<MarkdownTab, string>) => {
      setCoverSheetMarkdown(markdownContent.cover_sheet);
      setLessonPlanMarkdown(markdownContent.lesson_plan);
      setBackgroundKnowledgeMarkdown(markdownContent.background_knowledge);
      setBoardImageMarkdown(markdownContent.board_image);
      setExerciseMarkdown(markdownContent.exercise);
      setExerciseSolutionMarkdown(markdownContent.exercise_solution);
    },
    [],
  );

  const loadActivity = useCallback(async () => {
    if (!id) return;

    setIsLoadingActivity(true);
    setLoadError(null);
    try {
      const data = await apiService.getActivity(id);
      setActivity(data);

      const markdowns = await apiService.getActivityMarkdowns(id);
      const loadedMarkdownContent = getMarkdownContentByTab(markdowns);
      applyMarkdownContent(loadedMarkdownContent);
      setInitialMarkdownContent(loadedMarkdownContent);
    } catch (err) {
      logger.error("Failed to load activity", err, "ActivityEditPage");
      setLoadError(
        err instanceof Error ? err.message : "Failed to load activity",
      );
    } finally {
      setIsLoadingActivity(false);
    }
  }, [applyMarkdownContent, id]);

  useEffect(() => {
    void loadActivity();
  }, [loadActivity]);

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
    // exercise and exercise_solution are always generated together in one call
    const typesToRequest =
      activeMarkdownTab === "exercise" ||
      activeMarkdownTab === "exercise_solution"
        ? ["exercise", "exercise_solution"]
        : [activeMarkdownTab];
    try {
      const result = await apiService.generateActivityMarkdowns(
        documentId,
        savedMetadata as unknown as Record<string, unknown> | undefined,
        typesToRequest,
        activity?.id,
      );
      if (result.coverSheetMarkdown) {
        setCoverSheetMarkdown(result.coverSheetMarkdown);
      }
      if (result.lessonPlanMarkdown) {
        setLessonPlanMarkdown(result.lessonPlanMarkdown);
      }
      if (result.backgroundKnowledgeMarkdown) {
        setBackgroundKnowledgeMarkdown(result.backgroundKnowledgeMarkdown);
      }
      if (result.boardImageMarkdown) {
        setBoardImageMarkdown(result.boardImageMarkdown);
      }
      if (result.exerciseMarkdown) {
        setExerciseMarkdown(result.exerciseMarkdown);
      }
      if (result.exerciseSolutionMarkdown) {
        setExerciseSolutionMarkdown(result.exerciseSolutionMarkdown);
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

  const renderPreviewExercise = useCallback(
    (markdown: string) =>
      apiService.previewMarkdownPdf(
        markdown,
        "portrait",
        savedMetadata?.name || activity?.name || "",
        true,
      ),
    [activity?.name, savedMetadata?.name],
  );

  const renderPreviewTafelbild = useCallback(
    (markdown: string) =>
      apiService.previewMarkdownPdf(
        markdown,
        "portrait",
        savedMetadata?.name || activity?.name || "",
        false,
        true,
      ),
    [activity?.name, savedMetadata?.name],
  );

  // ─── Save ───────────────────────────────────────────────────────

  const handleSave = async () => {
    if (!savedMetadata || !id) return;

    setIsSaving(true);
    setSaveError(null);
    try {
      const updatePayload: UpdateActivityRequest = {
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
      };

      if (coverSheetMarkdown !== initialMarkdownContent.cover_sheet) {
        updatePayload.coverSheetMarkdown = coverSheetMarkdown;
      }
      if (lessonPlanMarkdown !== initialMarkdownContent.lesson_plan) {
        updatePayload.lessonPlanMarkdown = lessonPlanMarkdown;
      }
      if (
        backgroundKnowledgeMarkdown !==
        initialMarkdownContent.background_knowledge
      ) {
        updatePayload.backgroundKnowledgeMarkdown = backgroundKnowledgeMarkdown;
      }
      if (boardImageMarkdown !== initialMarkdownContent.board_image) {
        updatePayload.boardImageMarkdown = boardImageMarkdown;
      }
      if (exerciseMarkdown !== initialMarkdownContent.exercise) {
        updatePayload.exerciseMarkdown = exerciseMarkdown;
      }
      if (
        exerciseSolutionMarkdown !== initialMarkdownContent.exercise_solution
      ) {
        updatePayload.exerciseSolutionMarkdown = exerciseSolutionMarkdown;
      }

      await apiService.updateActivity(id, updatePayload);

      navigate(detailPath ?? `/library/${id}`, {
        state: detailNavigationState,
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
            onRetry={() => void loadActivity()}
          />
          <div className="mt-4">
            <Button onClick={handleBack}>{t("editActivity.goBack")}</Button>
          </div>
        </div>
      </div>
    );
  }

  const source = location.pathname.startsWith("/library/")
    ? "library"
    : location.pathname.startsWith("/recommendations/")
      ? "recommendations"
      : location.pathname.startsWith("/favourites/")
        ? "favourites"
        : location.pathname.startsWith("/drafts/")
          ? "drafts"
          : null;

  const scrollState =
    typeof navigationState?.restoreScrollY === "number"
      ? { restoreScrollY: navigationState.restoreScrollY }
      : undefined;

  const breadcrumbItems: BreadcrumbItem[] = [];
  if (source === "recommendations") {
    breadcrumbItems.push({
      label: t("nav.recommendations"),
      href: "/recommendations",
    });
    if (backTo) {
      breadcrumbItems.push({
        label: t("recommendations.results"),
        href: backTo,
        state: scrollState,
      });
    }
  } else if (source === "library") {
    breadcrumbItems.push({
      label: t("nav.library"),
      href: backTo ?? "/library",
      state: scrollState,
    });
  } else if (source === "favourites") {
    breadcrumbItems.push({
      label: t("nav.favourites"),
      href: backTo ?? "/favourites",
      state: scrollState,
    });
  } else if (source === "drafts") {
    breadcrumbItems.push({
      label: t("nav.drafts"),
      href: backTo ?? "/drafts",
      state: scrollState,
    });
  }
  if (detailPath) {
    breadcrumbItems.push({
      label: activity.name,
      href: detailPath,
      state: detailNavigationState,
    });
  }
  breadcrumbItems.push({ label: t("common.edit") });

  return (
    <div className="py-6">
      <Breadcrumb items={breadcrumbItems} className="mb-6" />

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
              ? () => handleBack()
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
                onCancel={handleBack}
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
                        {t(`markdownTypes.${key}`)}
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
                            doc: t(`markdownTypes.${activeMarkdownTab}`),
                          })
                        : t("upload.generate", {
                            doc: t(`markdownTypes.${activeMarkdownTab}`),
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
                    doc: t(`markdownTypes.${activeMarkdownTab}`),
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
              {activeMarkdownTab === "cover_sheet" && (
                <MarkdownEditorWithPreview
                  value={coverSheetMarkdown}
                  onChange={setCoverSheetMarkdown}
                  renderPreviewFn={renderPreviewPortrait}
                />
              )}
              {activeMarkdownTab === "lesson_plan" && (
                <MarkdownEditorWithPreview
                  value={lessonPlanMarkdown}
                  onChange={setLessonPlanMarkdown}
                  renderPreviewFn={renderPreviewLandscape}
                />
              )}
              {activeMarkdownTab === "background_knowledge" && (
                <MarkdownEditorWithPreview
                  value={backgroundKnowledgeMarkdown}
                  onChange={setBackgroundKnowledgeMarkdown}
                  renderPreviewFn={renderPreviewPortrait}
                />
              )}
              {activeMarkdownTab === "board_image" && (
                <MarkdownEditorWithPreview
                  value={boardImageMarkdown}
                  onChange={setBoardImageMarkdown}
                  renderPreviewFn={renderPreviewTafelbild}
                  onRegenerateImage={handleRegenerateImageForTafelbild}
                />
              )}
              {activeMarkdownTab === "exercise" && (
                <MarkdownEditorWithPreview
                  value={exerciseMarkdown}
                  onChange={setExerciseMarkdown}
                  renderPreviewFn={renderPreviewExercise}
                  onRegenerateImage={handleRegenerateImage}
                />
              )}
              {activeMarkdownTab === "exercise_solution" && (
                <MarkdownEditorWithPreview
                  value={exerciseSolutionMarkdown}
                  onChange={setExerciseSolutionMarkdown}
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
