import React, { useState, useCallback, useRef, useEffect } from "react";
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
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  Upload,
  FileText,
  CheckCircle,
  AlertCircle,
  Loader2,
  ArrowLeft,
  Save,
  Eye,
  Edit3,
} from "lucide-react";
import { apiService } from "@/services/apiService";
import { ActivityForm } from "@/components/forms/ActivityForm";
import { logger } from "@/services/logger";
import type { Activity } from "@/types/activity";
import type { FormFieldData } from "@/types/api";

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

type Step = "upload" | "metadata" | "artikulationsschema";

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

  // Metadata form state (saved when moving to step 2)
  const [savedMetadata, setSavedMetadata] = useState<ActivityFormData | null>(
    null,
  );

  // Artikulationsschema state
  const [artikulationsschemaMarkdown, setArtikulationsschemaMarkdown] =
    useState<string>("");
  const [isGeneratingSchema, setIsGeneratingSchema] = useState(false);
  const [schemaError, setSchemaError] = useState<string | null>(null);

  // PDF preview state
  const [previewPdfUrl, setPreviewPdfUrl] = useState<string | null>(null);
  const [isRenderingPreview, setIsRenderingPreview] = useState(false);
  const [isPreviewModalOpen, setIsPreviewModalOpen] = useState(false);

  // Upload state
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [isUploading, setIsUploading] = useState(false);
  const [uploadError, setUploadError] = useState<string | null>(null);
  const [dragActive, setDragActive] = useState(false);

  // Final save state
  const [isSaving, setIsSaving] = useState(false);

  // Debounce ref for preview rendering
  const debounceTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  // Cleanup blob URL on unmount or change
  useEffect(() => {
    return () => {
      if (previewPdfUrl) {
        URL.revokeObjectURL(previewPdfUrl);
      }
    };
  }, [previewPdfUrl]);

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
      const result = await apiService.uploadPdfDraft(selectedFile);
      setDocumentId(result.document_id);
      setExtractedData(result.extracted_data);
      setExtractionConfidence(result.extraction_confidence);
      setExtractionQuality(result.extraction_quality);
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

  const handleMetadataNext = async (formData: ActivityFormData) => {
    setSavedMetadata(formData);
    setCurrentStep("artikulationsschema");

    // Generate Artikulationsschema if not already generated
    if (!artikulationsschemaMarkdown && documentId) {
      setIsGeneratingSchema(true);
      setSchemaError(null);
      try {
        const result =
          await apiService.generateArtikulationsschema(documentId);
        setArtikulationsschemaMarkdown(result.markdown);
        // Trigger initial preview
        debouncedRenderPreview(result.markdown);
      } catch (error) {
        logger.error(
          "Schema generation error",
          error,
          "ActivitySetupPage",
        );
        setSchemaError(
          error instanceof Error
            ? error.message
            : "Failed to generate Artikulationsschema",
        );
      } finally {
        setIsGeneratingSchema(false);
      }
    } else if (artikulationsschemaMarkdown) {
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
      logger.error("Preview render error", error, "ActivitySetupPage");
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

  // ─── Final Save ─────────────────────────────────────────────────

  const handleSave = async () => {
    if (!savedMetadata || !documentId) return;

    setIsSaving(true);
    try {
      const response = (await apiService.createActivity({
        ...savedMetadata,
        document_id: documentId,
        artikulationsschema_markdown:
          artikulationsschemaMarkdown || undefined,
      })) as { activity: Activity };

      navigate(`/activity-details/${response.activity.id}`);
    } catch (error) {
      logger.error("Save error", error, "ActivitySetupPage");
      setSchemaError(
        error instanceof Error
          ? error.message
          : "Failed to save activity",
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
      key: "artikulationsschema" as Step,
      label: "Artikulationsschema",
      number: 2,
    },
  ];

  const currentStepIndex = steps.findIndex((s) => s.key === currentStep);

  // ─── Render ─────────────────────────────────────────────────────

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

      {/* Step: Upload */}
      {currentStep === "upload" && (
        <div className="max-w-2xl mx-auto">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Upload className="h-5 w-5" />
                Upload Activity PDF
              </CardTitle>
              <CardDescription>
                Upload a PDF file containing learning activity information.
                The system will extract metadata and generate an
                Artikulationsschema.
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

              {uploadError && (
                <div className="flex items-center gap-2 p-3 bg-destructive/10 border border-destructive/20 rounded-lg">
                  <AlertCircle className="h-4 w-4 text-destructive" />
                  <p className="text-sm text-destructive">{uploadError}</p>
                </div>
              )}

              <Button
                onClick={handleUploadAndExtract}
                disabled={!selectedFile || isUploading}
                className="w-full"
              >
                {isUploading ? (
                  <>
                    <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                    Uploading &amp; Extracting...
                  </>
                ) : (
                  <>
                    <Upload className="h-4 w-4 mr-2" />
                    Upload &amp; Extract Metadata
                  </>
                )}
              </Button>
            </CardContent>
          </Card>
        </div>
      )}

      {/* Step: Metadata Review */}
      {currentStep === "metadata" && (
        <div className="max-w-2xl mx-auto">
          <Card>
            <CardHeader>
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
                          : "bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400"
                    }`}
                  >
                    {extractionQuality} quality (
                    {(extractionConfidence * 100).toFixed(0)}%)
                  </span>
                )}
              </CardDescription>
            </CardHeader>
            <CardContent>
              <ActivityForm
                initialData={
                  savedMetadata ||
                  ({
                    ...extractedData,
                    document_id: documentId || null,
                  } as Partial<ActivityFormData>)
                }
                onSubmit={handleMetadataNext}
                onCancel={() => setCurrentStep("upload")}
                isLoading={false}
                submitLabel="Next: Artikulationsschema"
                cancelLabel="Back"
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
                  Save Activity
                </>
              )}
            </Button>
          </div>

          {isGeneratingSchema ? (
            <Card>
              <CardContent className="flex flex-col items-center justify-center py-16">
                <Loader2 className="h-8 w-8 animate-spin text-primary mb-4" />
                <p className="text-lg font-medium">
                  Generating Artikulationsschema...
                </p>
                <p className="text-sm text-muted-foreground mt-1">
                  The AI is analyzing the PDF and creating a lesson
                  articulation schema.
                </p>
              </CardContent>
            </Card>
          ) : schemaError ? (
            <Card>
              <CardContent className="py-8">
                <div className="flex flex-col items-center gap-4">
                  <AlertCircle className="h-8 w-8 text-destructive" />
                  <p className="text-destructive font-medium">
                    {schemaError}
                  </p>
                  <Button
                    variant="outline"
                    onClick={() => {
                      if (documentId) {
                        setIsGeneratingSchema(true);
                        setSchemaError(null);
                        apiService
                          .generateArtikulationsschema(documentId)
                          .then((result) => {
                            setArtikulationsschemaMarkdown(
                              result.markdown,
                            );
                            debouncedRenderPreview(result.markdown);
                          })
                          .catch((err) =>
                            setSchemaError(
                              err instanceof Error
                                ? err.message
                                : "Retry failed",
                            ),
                          )
                          .finally(() =>
                            setIsGeneratingSchema(false),
                          );
                      }
                    }}
                  >
                    Retry Generation
                  </Button>
                </div>
              </CardContent>
            </Card>
          ) : (
            <>
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
            </>
          )}
        </div>
      )}
    </div>
  );
};
