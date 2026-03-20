import React, { useState, useCallback, useMemo } from "react";
import { useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import {
  Upload,
  FileText,
  AlertCircle,
  Loader2,
  ArrowRight,
  Save,
  Edit3,
  RefreshCw,
  Sparkles,
} from "lucide-react";
import { apiService } from "@/services/apiService";
import {
  ActivityForm,
  type ActivityFormData,
} from "@/components/forms/ActivityForm";
import { StepIndicator } from "@/components/ui/StepIndicator";
import { PageHeader } from "@/components/ui/PageHeader";
import { MarkdownEditorWithPreview } from "@/components/ui/MarkdownEditorWithPreview";
import { logger } from "@/services/logger";
import type { Activity } from "@/types/activity";
import type { FormFieldData } from "@/types/api";
import { Checkbox } from "@/components/ui/checkbox";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

type Step = "upload" | "metadata" | "documents";
type MarkdownTab = "deckblatt" | "artikulationsschema" | "hintergrundwissen";

const MARKDOWN_TAB_LABELS: Record<MarkdownTab, string> = {
  deckblatt: "Deckblatt",
  artikulationsschema: "Artikulationsschema",
  hintergrundwissen: "Hintergrundwissen",
};

// ─── Component ───────────────────────────────────────────────────

export const ActivitySetupPage: React.FC = () => {
  const navigate = useNavigate();

  // Flow state
  const [currentStep, setCurrentStep] = useState<Step>("upload");
  const [documentId, setDocumentId] = useState<string | null>(null);
  const [extractedData, setExtractedData] = useState<FormFieldData | null>(
    null,
  );
  const [extractionQuality, setExtractionQuality] = useState<string>("");
  const [extractionConfidence, setExtractionConfidence] = useState<number>(0);
  const [shouldExtractMetadata, setShouldExtractMetadata] = useState(true);
  const [shouldGenerateSchema, setShouldGenerateSchema] = useState(true);

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
  const [isGeneratingSchema, setIsGeneratingSchema] = useState(false);
  const [isRegeneratingMetadata, setIsRegeneratingMetadata] = useState(false);
  const [schemaError, setSchemaError] = useState<string | null>(null);
  const [hasVisitedSchemaStep, setHasVisitedSchemaStep] = useState(false);

  // Upload state
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [isUploading, setIsUploading] = useState(false);
  const [uploadError, setUploadError] = useState<string | null>(null);
  const [dragActive, setDragActive] = useState(false);

  // Final save state
  const [isSaving, setIsSaving] = useState(false);

  /** Whether the currently active markdown tab already has content */
  const activeTabHasContent =
    (activeMarkdownTab === "deckblatt" && !!deckblattMarkdown) ||
    (activeMarkdownTab === "artikulationsschema" &&
      !!artikulationsschemaMarkdown) ||
    (activeMarkdownTab === "hintergrundwissen" && !!hintergrundwissenMarkdown);

  // ─── Upload Handlers ────────────────────────────────────────────

  const handleFileSelect = (file: File) => {
    if (file.type !== "application/pdf") {
      setUploadError("Please select a PDF file");
      return;
    }
    if (file.size === 0) {
      setUploadError("The selected file is empty");
      return;
    }
    if (file.size > 10 * 1024 * 1024) {
      setUploadError("File size must be less than 10MB");
      return;
    }
    setSelectedFile(file);
    setUploadError(null);
  };

  const handleFileInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) handleFileSelect(file);
  };

  const handleDrag = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.type === "dragenter" || e.type === "dragover") {
      setDragActive(true);
    } else if (e.type === "dragleave") {
      setDragActive(false);
    }
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(false);
    const file = e.dataTransfer.files?.[0];
    if (file) handleFileSelect(file);
  };

  const handleUploadAndExtract = async () => {
    if (!selectedFile) {
      setUploadError("Please select a file first");
      return;
    }

    setIsUploading(true);
    setUploadError(null);

    try {
      const result = await apiService.uploadPdfDraft(selectedFile, {
        extractMetadata: shouldExtractMetadata,
      });
      setDocumentId(result.documentId);
      setExtractedData(result.extractedData);
      setExtractionConfidence(result.extractionConfidence);
      setExtractionQuality(result.extractionQuality);
      setSavedMetadata(null);
      setArtikulationsschemaMarkdown("");
      setDeckblattMarkdown("");
      setHintergrundwissenMarkdown("");
      setSchemaError(null);
      setHasVisitedSchemaStep(false);
      setCurrentStep("metadata");
    } catch (error) {
      logger.error("Upload error", error, "ActivitySetupPage");
      setUploadError(
        error instanceof Error
          ? error.message
          : "Failed to upload and process PDF",
      );
    } finally {
      setIsUploading(false);
    }
  };

  // ─── Metadata Step Handlers ─────────────────────────────────────

  const generateAllMarkdowns = async (metadata?: ActivityFormData) => {
    if (!documentId) return;

    setIsGeneratingSchema(true);
    setSchemaError(null);
    try {
      const result = await apiService.generateActivityMarkdowns(
        documentId,
        metadata as unknown as Record<string, unknown> | undefined,
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
    } catch (error) {
      logger.error("Schema generation error", error, "ActivitySetupPage");
      setSchemaError(
        error instanceof Error ? error.message : "Failed to generate documents",
      );
    } finally {
      setIsGeneratingSchema(false);
    }
  };

  const generateActiveMarkdown = async () => {
    if (!documentId) return;

    setIsGeneratingSchema(true);
    setSchemaError(null);
    try {
      const result = await apiService.generateActivityMarkdowns(
        documentId,
        savedMetadata as unknown as Record<string, unknown> | undefined,
        [activeMarkdownTab],
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
    } catch (error) {
      logger.error("Schema generation error", error, "ActivitySetupPage");
      setSchemaError(
        error instanceof Error ? error.message : "Failed to generate documents",
      );
    } finally {
      setIsGeneratingSchema(false);
    }
  };

  const handleRegenerateMetadata = async () => {
    if (!documentId) return;

    setIsRegeneratingMetadata(true);
    setUploadError(null);
    try {
      const result = await apiService.regenerateMetadata(documentId);
      setExtractedData(result.extractedData);
      setExtractionConfidence(result.extractionConfidence);
      setExtractionQuality(result.extractionQuality);
      setSavedMetadata(null);
      setArtikulationsschemaMarkdown("");
      setDeckblattMarkdown("");
      setHintergrundwissenMarkdown("");
      setSchemaError(null);
    } catch (error) {
      logger.error("Metadata regeneration error", error, "ActivitySetupPage");
      setUploadError(
        error instanceof Error
          ? error.message
          : "Failed to regenerate metadata",
      );
    } finally {
      setIsRegeneratingMetadata(false);
    }
  };

  const handleMetadataNext = async (formData: ActivityFormData) => {
    const shouldAutoGenerateOnThisVisit = !hasVisitedSchemaStep;

    setSavedMetadata(formData);
    setHasVisitedSchemaStep(true);
    setCurrentStep("documents");

    const hasAnyMarkdown =
      artikulationsschemaMarkdown ||
      deckblattMarkdown ||
      hintergrundwissenMarkdown;

    if (
      shouldAutoGenerateOnThisVisit &&
      shouldGenerateSchema &&
      !hasAnyMarkdown
    ) {
      await generateAllMarkdowns(formData);
    }
  };

  // ─── Preview Rendering ──────────────────────────────────────────

  const previewActivityName =
    typeof metadataInitialData.name === "string" ? metadataInitialData.name : "";

  const renderPreviewLandscape = useCallback(
    (markdown: string) =>
      apiService.previewMarkdownPdf(
        markdown,
        "landscape",
        previewActivityName,
      ),
    [previewActivityName],
  );

  const renderPreviewPortrait = useCallback(
    (markdown: string) =>
      apiService.previewMarkdownPdf(markdown, "portrait", previewActivityName),
    [previewActivityName],
  );

  // ─── Final Save ─────────────────────────────────────────────────

  const handleSave = async () => {
    if (!savedMetadata || !documentId) return;

    setIsSaving(true);
    try {
      const response = (await apiService.createActivity({
        ...savedMetadata,
        documentId: documentId,
        artikulationsschemaMarkdown: artikulationsschemaMarkdown || undefined,
        deckblattMarkdown: deckblattMarkdown || undefined,
        hintergrundwissenMarkdown: hintergrundwissenMarkdown || undefined,
      })) as { activity: Activity };

      navigate(`/activity-details/${response.activity.id}`);
    } catch (error) {
      logger.error("Save error", error, "ActivitySetupPage");
      setSchemaError(
        error instanceof Error ? error.message : "Failed to save activity",
      );
    } finally {
      setIsSaving(false);
    }
  };

  // ─── Step Indicator ─────────────────────────────────────────────

  const steps = [
    { key: "upload" as Step, label: "Upload PDF", number: 0 },
    { key: "metadata" as Step, label: "Review Metadata", number: 1 },
    {
      key: "documents" as Step,
      label: "Documents",
      number: 2,
    },
  ];

  const currentStepIndex = steps.findIndex((s) => s.key === currentStep);
  const metadataInitialData = useMemo(
    () =>
      savedMetadata ||
      ({
        ...(extractedData ?? {}),
        documentId: documentId || null,
      } as Partial<ActivityFormData>),
    [documentId, extractedData, savedMetadata],
  );

  // ─── Render ─────────────────────────────────────────────────────

  return (
    <div className="w-full py-6">
      {/* Page Header & Step Indicator */}
      <div className="space-y-6 mb-8">
        <PageHeader
          title="Create Activity"
          description="Upload a PDF, review the extracted metadata, and finalize the Artikulationsschema."
        />
        <StepIndicator
          steps={steps}
          currentStepIndex={currentStepIndex}
          onBack={
            currentStep === "metadata"
              ? () => setCurrentStep("upload")
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
                  formId: "activity-setup-form",
                }
              : currentStep === "documents"
                ? {
                    label: "Save Activity",
                    variant: "default",
                    onClick: handleSave,
                    icon: <Save className="h-4 w-4" />,
                    loading: isSaving,
                    loadingLabel: "Saving...",
                    disabled: isSaving || isGeneratingSchema,
                  }
                : undefined
          }
        />
      </div>

      {/* Step: Upload */}
      {currentStep === "upload" && (
        <div>
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Upload className="h-5 w-5" />
                Upload Activity PDF
              </CardTitle>
              <CardDescription>
                Upload a PDF file containing learning activity information. The
                system can extract metadata and generate an Artikulationsschema
                for you.
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="pdf-file">Select PDF File</Label>
                <Input
                  id="pdf-file"
                  type="file"
                  accept=".pdf"
                  onChange={handleFileInputChange}
                  className="cursor-pointer"
                />
              </div>

              <div
                className={`border-2 border-dashed rounded-lg p-8 text-center transition-colors ${
                  dragActive
                    ? "border-primary bg-primary/5"
                    : "border-border hover:border-primary/50"
                }`}
                onDragEnter={handleDrag}
                onDragLeave={handleDrag}
                onDragOver={handleDrag}
                onDrop={handleDrop}
              >
                <FileText className="h-12 w-12 mx-auto mb-4 text-muted-foreground" />
                <p className="text-lg font-medium mb-2">
                  Drag and drop your PDF here
                </p>
                <p className="text-sm text-muted-foreground mb-4">
                  or click the file input above
                </p>
                <p className="text-xs text-muted-foreground">
                  Maximum file size: 10MB
                </p>
              </div>

              {selectedFile && (
                <div className="flex items-center gap-2 p-3 bg-muted rounded-lg">
                  <FileText className="h-4 w-4 text-primary" />
                  <span className="text-sm font-medium">
                    {selectedFile.name}
                  </span>
                  <span className="text-xs text-muted-foreground ml-auto">
                    {(selectedFile.size / 1024).toFixed(1)} KB
                  </span>
                </div>
              )}

              <div className="rounded-lg border bg-muted/30 p-4 space-y-3">
                <div>
                  <p className="text-sm font-medium">AI options</p>
                  <p className="text-xs text-muted-foreground">
                    Choose which AI-assisted steps should run automatically.
                  </p>
                </div>
                <label className="flex items-start gap-3 cursor-pointer">
                  <Checkbox
                    checked={shouldExtractMetadata}
                    onCheckedChange={(checked) =>
                      setShouldExtractMetadata(checked === true)
                    }
                  />
                  <div className="space-y-1">
                    <span className="text-sm font-medium">
                      Extract metadata from PDF
                    </span>
                    <p className="text-xs text-muted-foreground">
                      Prefill the activity form with AI-extracted fields.
                    </p>
                  </div>
                </label>
                <label className="flex items-start gap-3 cursor-pointer">
                  <Checkbox
                    checked={shouldGenerateSchema}
                    onCheckedChange={(checked) =>
                      setShouldGenerateSchema(checked === true)
                    }
                  />
                  <div className="space-y-1">
                    <span className="text-sm font-medium">
                      Generate documents after review
                    </span>
                    <p className="text-xs text-muted-foreground">
                      Run AI generation of Deckblatt, Artikulationsschema, and
                      Hintergrundwissen automatically when you finish the
                      metadata step.
                    </p>
                  </div>
                </label>
              </div>

              {uploadError && (
                <div className="flex items-center gap-2 p-3 bg-destructive/10 border border-destructive/20 rounded-lg">
                  <AlertCircle className="h-4 w-4 text-destructive" />
                  <p className="text-sm text-destructive">{uploadError}</p>
                </div>
              )}

              <Button
                onClick={handleUploadAndExtract}
                disabled={!selectedFile || isUploading}
                className="w-full gap-2"
              >
                {isUploading ? (
                  <>
                    <Loader2 className="h-4 w-4 animate-spin" />
                    {shouldExtractMetadata
                      ? "Uploading & Extracting..."
                      : "Uploading..."}
                  </>
                ) : (
                  <>
                    <Upload className="h-4 w-4" />
                    {shouldExtractMetadata
                      ? "Upload & Extract Metadata"
                      : "Upload PDF"}
                    <ArrowRight className="h-4 w-4 ml-1" />
                  </>
                )}
              </Button>
            </CardContent>
          </Card>
        </div>
      )}

      {/* Step: Metadata Review */}
      {currentStep === "metadata" && (
        <div>
          <Card>
            <CardHeader>
              <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
                <div>
                  <CardTitle className="flex items-center gap-2">
                    <Edit3 className="h-5 w-5" />
                    Review Extracted Metadata
                  </CardTitle>
                  <CardDescription>
                    Review and edit the metadata extracted from your PDF.
                    {extractionQuality && (
                      <span
                        className={`ml-2 inline-flex items-center px-2 py-0.5 rounded text-xs font-medium ${
                          extractionQuality === "high"
                            ? "bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400"
                            : extractionQuality === "medium"
                              ? "bg-yellow-100 text-yellow-700 dark:bg-yellow-900/30 dark:text-yellow-400"
                              : extractionQuality === "not_run"
                                ? "bg-slate-100 text-slate-700 dark:bg-slate-900/30 dark:text-slate-300"
                                : "bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400"
                        }`}
                      >
                        {extractionQuality === "not_run"
                          ? "AI skipped"
                          : `${extractionQuality} quality (${(
                              extractionConfidence * 100
                            ).toFixed(0)}%)`}
                      </span>
                    )}
                  </CardDescription>
                </div>
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
                      Regenerating...
                    </>
                  ) : (
                    <>
                      <RefreshCw className="h-4 w-4" />
                      Re-run AI metadata
                    </>
                  )}
                </Button>
              </div>
            </CardHeader>
            <CardContent className="space-y-4">
              {uploadError && (
                <div className="flex items-center gap-2 p-3 bg-destructive/10 border border-destructive/20 rounded-lg">
                  <AlertCircle className="h-4 w-4 text-destructive" />
                  <p className="text-sm text-destructive">{uploadError}</p>
                </div>
              )}
              <ActivityForm
                initialData={metadataInitialData}
                onSubmit={handleMetadataNext}
                onCancel={() => setCurrentStep("upload")}
                isLoading={false}
                hideButtons
                formId="activity-setup-form"
              />
            </CardContent>
          </Card>
        </div>
      )}

      {/* Step: Documents (Deckblatt, Artikulationsschema, Hintergrundwissen) */}
      {currentStep === "documents" && (
        <div className="space-y-4 lg:relative lg:left-1/2 lg:w-[calc(100vw-16rem-4rem)] lg:max-w-none lg:-translate-x-1/2">
          <Card>
            <CardContent className="flex flex-col gap-3 py-4 sm:flex-row sm:items-center sm:justify-between">
              <div className="flex items-center gap-3">
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
                <p className="text-sm text-muted-foreground hidden sm:block">
                  Edit the AI draft or generate a fresh version.
                </p>
              </div>
              <Button
                type="button"
                variant="outline"
                className="gap-2"
                onClick={() => void generateActiveMarkdown()}
                disabled={!documentId || !savedMetadata || isGeneratingSchema}
              >
                {isGeneratingSchema ? (
                  <>
                    <Loader2 className="h-4 w-4 animate-spin" />
                    Generating...
                  </>
                ) : (
                  <>
                    {activeTabHasContent ? (
                      <RefreshCw className="h-4 w-4" />
                    ) : (
                      <Sparkles className="h-4 w-4" />
                    )}
                    {activeTabHasContent
                      ? `Re-generate ${MARKDOWN_TAB_LABELS[activeMarkdownTab]}`
                      : `Generate ${MARKDOWN_TAB_LABELS[activeMarkdownTab]}`}
                  </>
                )}
              </Button>
            </CardContent>
          </Card>
          {isGeneratingSchema ? (
            <Card>
              <CardContent className="flex flex-col items-center justify-center py-16">
                <Loader2 className="h-8 w-8 animate-spin text-primary mb-4" />
                <p className="text-lg font-medium">
                  Generating {MARKDOWN_TAB_LABELS[activeMarkdownTab]}...
                </p>
                <p className="text-sm text-muted-foreground mt-1">
                  The AI is analyzing the PDF and creating the document.
                </p>
              </CardContent>
            </Card>
          ) : schemaError ? (
            <Card>
              <CardContent className="py-8">
                <div className="flex flex-col items-center gap-4">
                  <AlertCircle className="h-8 w-8 text-destructive" />
                  <p className="text-destructive font-medium">{schemaError}</p>
                  <Button
                    variant="outline"
                    onClick={() => {
                      void generateActiveMarkdown();
                    }}
                  >
                    Retry Generation
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
            </>
          )}
        </div>
      )}
    </div>
  );
};
