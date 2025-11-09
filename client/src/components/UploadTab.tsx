import React, { useState } from "react";
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
  CheckCircle,
  AlertCircle,
  Loader2,
  Plus,
  RefreshCw,
} from "lucide-react";
import { apiService } from "@/services/apiService";
import { ActivityCreationModal } from "./ActivityCreationModal";
import type { Activity } from "@/types/activity";
import { logger } from "@/services/logger";
import type { FormFieldData } from "@/types/api";

interface UploadedDocument {
  id: number;
  filename: string;
  file_size: number;
  extracted_fields: FormFieldData & {
    name?: string;
    description?: string;
    age_min?: number;
    age_max?: number;
    format?: string;
    bloom_level?: string;
    duration_min_minutes?: number;
    duration_max_minutes?: number;
    topics?: string[];
    resources_needed?: string[];
  };
  confidence: number; // Overall confidence level (0.0 to 1.0)
  extraction_quality: string;
  created_at: string;
}

interface UploadResult {
  success: boolean;
  message: string;
  document?: UploadedDocument;
}

export const UploadTab: React.FC = () => {
  const navigate = useNavigate();
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [isUploading, setIsUploading] = useState(false);
  const [isProcessing, setIsProcessing] = useState(false);
  const [uploadedDocumentId, setUploadedDocumentId] = useState<number | null>(
    null,
  );
  const [processingError, setProcessingError] = useState<string | null>(null);
  const [uploadResult, setUploadResult] = useState<UploadResult | null>(null);
  const [dragActive, setDragActive] = useState(false);
  const [showActivityModal, setShowActivityModal] = useState(false);
  const [isManualEntry, setIsManualEntry] = useState(false);

  const handleFileSelect = (file: File) => {
    if (file.type !== "application/pdf") {
      setUploadResult({
        success: false,
        message: "Please select a PDF file",
      });
      return;
    }

    if (file.size === 0) {
      setUploadResult({
        success: false,
        message: "The selected file is empty",
      });
      return;
    }

    if (file.size > 10 * 1024 * 1024) {
      // 10MB limit
      setUploadResult({
        success: false,
        message: "File size must be less than 10MB",
      });
      return;
    }

    setSelectedFile(file);
    setUploadResult(null);
  };

  const handleFileInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      handleFileSelect(file);
    }
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
    if (file) {
      handleFileSelect(file);
    }
  };

  const handleProcessPdf = async (
    documentId: number,
    filename: string,
    fileSize: number,
  ) => {
    setIsProcessing(true);
    setProcessingError(null);

    try {
      // Process the PDF to extract activity data
      const processResponse = (await apiService.processPdf(documentId)) as {
        extracted_data: Record<string, unknown>;
        confidence: number;
        extraction_quality: string;
      };

      setUploadResult({
        success: true,
        message: "PDF uploaded and processed successfully",
        document: {
          id: documentId,
          filename: filename,
          file_size: fileSize,
          extracted_fields: processResponse.extracted_data,
          confidence: processResponse.confidence,
          extraction_quality: processResponse.extraction_quality,
          created_at: new Date().toISOString(),
        } as UploadedDocument,
      });
      setSelectedFile(null);
      // Reset file input
      const fileInput = document.getElementById("pdf-file") as HTMLInputElement;
      if (fileInput) fileInput.value = "";
      setUploadedDocumentId(null);
    } catch (error) {
      logger.error("Processing error", error, "UploadTab");
      setProcessingError(
        error instanceof Error
          ? error.message
          : "An unexpected error occurred during PDF processing",
      );
    } finally {
      setIsProcessing(false);
    }
  };

  const handleUpload = async () => {
    if (!selectedFile) {
      setUploadResult({
        success: false,
        message: "Please select a file to upload",
      });
      return;
    }

    setIsUploading(true);
    setUploadResult(null);
    setProcessingError(null);

    try {
      // Upload the PDF
      const uploadResponse = (await apiService.uploadPdf(selectedFile)) as {
        document_id: number;
        filename: string;
        file_size: number;
      };
      const documentId = uploadResponse.document_id;
      setUploadedDocumentId(documentId);
      setIsUploading(false);

      // Now process the PDF
      await handleProcessPdf(
        documentId,
        uploadResponse.filename,
        uploadResponse.file_size,
      );
    } catch (error) {
      logger.error("Upload error", error, "UploadTab");
      setUploadResult({
        success: false,
        message:
          error instanceof Error
            ? error.message
            : "An unexpected error occurred during file upload",
      });
      setIsUploading(false);
      setUploadedDocumentId(null);
    }
  };

  const handleSkip = () => {
    // Stop processing and show manual entry form
    setIsProcessing(false);
    setIsManualEntry(true);
    setShowActivityModal(true);
  };

  const handleRetry = () => {
    if (uploadedDocumentId) {
      // Reset manual entry flag when retrying
      setIsManualEntry(false);
      // Get the filename and file size from the selected file
      const filename = selectedFile?.name || "document.pdf";
      const fileSize = selectedFile?.size || 0;
      handleProcessPdf(uploadedDocumentId, filename, fileSize);
    }
  };

  const handleManualEntry = () => {
    setIsManualEntry(true);
    setShowActivityModal(true);
  };

  const formatFileSize = (bytes: number): string => {
    if (bytes === 0) return "0 Bytes";
    const k = 1024;
    const sizes = ["Bytes", "KB", "MB", "GB"];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + " " + sizes[i];
  };

  const formatDate = (dateString: string): string => {
    return new Date(dateString).toLocaleString();
  };

  return (
    <div className="w-full max-w-4xl mx-auto space-y-6">
      <div className="text-center">
        <h2 className="text-2xl font-bold text-foreground mb-2">
          Upload Activity PDF
        </h2>
        <p className="text-muted-foreground">
          Upload a PDF file to extract activity information automatically
        </p>
      </div>

      {/* Upload Area */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Upload className="h-5 w-5" />
            PDF Upload
          </CardTitle>
          <CardDescription>
            Select a PDF file containing activity information. The system will
            automatically extract structured data.
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          {/* File Input */}
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

          {/* Drag and Drop Area */}
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

          {/* Selected File Info */}
          {selectedFile && (
            <div className="p-4 bg-muted/50 rounded-lg">
              <div className="flex items-center gap-2 mb-2">
                <FileText className="h-4 w-4 text-primary" />
                <span className="font-medium">{selectedFile.name}</span>
              </div>
              <p className="text-sm text-muted-foreground">
                Size: {formatFileSize(selectedFile.size)}
              </p>
            </div>
          )}

          {/* Upload Button */}
          <Button
            onClick={handleUpload}
            disabled={!selectedFile || isUploading || isProcessing}
            className="w-full"
          >
            {isUploading ? (
              <>
                <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                Uploading PDF...
              </>
            ) : isProcessing ? (
              <>
                <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                Processing with AI...
              </>
            ) : (
              <>
                <Upload className="h-4 w-4 mr-2" />
                Upload PDF
              </>
            )}
          </Button>

          {/* Skip Button - shown during processing */}
          {isProcessing && (
            <Button onClick={handleSkip} variant="outline" className="w-full">
              Skip and Enter Manually
            </Button>
          )}

          {/* Processing Error - shown when extraction fails */}
          {processingError && uploadedDocumentId && (
            <div className="mt-4 p-4 bg-destructive/10 border border-destructive/20 rounded-lg">
              <div className="flex items-start gap-3 mb-3">
                <AlertCircle className="h-5 w-5 text-destructive mt-0.5 flex-shrink-0" />
                <div className="flex-1">
                  <p className="font-medium text-destructive">
                    Processing Failed
                  </p>
                  <p className="text-sm text-destructive/80 mt-1">
                    {processingError}
                  </p>
                </div>
              </div>
              <div className="flex gap-2">
                <Button
                  onClick={handleRetry}
                  variant="outline"
                  className="flex-1"
                  disabled={isProcessing}
                >
                  <RefreshCw className="h-4 w-4 mr-2" />
                  Retry Extraction
                </Button>
                <Button onClick={handleManualEntry} className="flex-1">
                  <Plus className="h-4 w-4 mr-2" />
                  Create Manually
                </Button>
              </div>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Upload Result */}
      {uploadResult && (
        <Card>
          <CardContent className="pt-6">
            <div
              className={`flex items-start gap-3 p-4 rounded-lg ${
                uploadResult.success
                  ? "bg-success/10 border border-success/20"
                  : "bg-destructive/10 border border-destructive/20"
              }`}
            >
              {uploadResult.success ? (
                <CheckCircle className="h-5 w-5 text-success mt-0.5" />
              ) : (
                <AlertCircle className="h-5 w-5 text-destructive mt-0.5" />
              )}
              <div className="flex-1">
                <p
                  className={`font-medium ${
                    uploadResult.success ? "text-success" : "text-destructive"
                  }`}
                >
                  {uploadResult.message}
                </p>

                {uploadResult.success && uploadResult.document && (
                  <div className="mt-4 space-y-4">
                    <h4 className="font-medium text-foreground">
                      Extracted Information:
                    </h4>

                    {/* Document metadata in a more compact layout */}
                    <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-3 text-sm">
                      <div className="flex flex-col">
                        <span className="font-medium text-muted-foreground">
                          Document ID
                        </span>
                        <span className="text-foreground">
                          {uploadResult.document.id}
                        </span>
                      </div>
                      <div className="flex flex-col">
                        <span className="font-medium text-muted-foreground">
                          File Size
                        </span>
                        <span className="text-foreground">
                          {formatFileSize(uploadResult.document.file_size)}
                        </span>
                      </div>
                      <div className="flex flex-col">
                        <span className="font-medium text-muted-foreground">
                          Confidence
                        </span>
                        <span className="text-foreground">
                          {uploadResult.document.confidence !== undefined
                            ? `${Math.round(uploadResult.document.confidence * 100)}%`
                            : "N/A"}
                        </span>
                      </div>
                      <div className="flex flex-col">
                        <span className="font-medium text-muted-foreground">
                          Quality
                        </span>
                        <span className="text-foreground">
                          {uploadResult.document.extraction_quality || "N/A"}
                        </span>
                      </div>
                      <div className="flex flex-col">
                        <span className="font-medium text-muted-foreground">
                          Uploaded
                        </span>
                        <span className="text-foreground">
                          {formatDate(uploadResult.document.created_at)}
                        </span>
                      </div>
                    </div>

                    {uploadResult.document.extracted_fields && (
                      <div className="mt-4">
                        <h5 className="font-medium text-foreground mb-3">
                          Extracted Activity Data:
                        </h5>
                        <div className="bg-muted/30 rounded-lg p-4 space-y-3">
                          {uploadResult.document.extracted_fields.name && (
                            <div>
                              <span className="font-medium text-muted-foreground text-sm">
                                Activity Name:
                              </span>
                              <p className="text-foreground font-medium mt-1">
                                {uploadResult.document.extracted_fields.name}
                              </p>
                            </div>
                          )}
                          {uploadResult.document.extracted_fields
                            .description && (
                            <div>
                              <span className="font-medium text-muted-foreground text-sm">
                                Description:
                              </span>
                              <p className="text-foreground mt-1 text-sm leading-relaxed">
                                {
                                  uploadResult.document.extracted_fields
                                    .description
                                }
                              </p>
                            </div>
                          )}
                          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3 text-sm">
                            {uploadResult.document.extracted_fields.age_min &&
                              uploadResult.document.extracted_fields
                                .age_max && (
                                <div>
                                  <span className="font-medium text-muted-foreground">
                                    Age Range:
                                  </span>
                                  <span className="text-foreground ml-2">
                                    {
                                      uploadResult.document.extracted_fields
                                        .age_min
                                    }
                                    -
                                    {
                                      uploadResult.document.extracted_fields
                                        .age_max
                                    }{" "}
                                    years
                                  </span>
                                </div>
                              )}
                            {uploadResult.document.extracted_fields.format && (
                              <div>
                                <span className="font-medium text-muted-foreground">
                                  Format:
                                </span>
                                <span className="text-foreground ml-2 capitalize">
                                  {
                                    uploadResult.document.extracted_fields
                                      .format
                                  }
                                </span>
                              </div>
                            )}
                            {uploadResult.document.extracted_fields
                              .bloom_level && (
                              <div>
                                <span className="font-medium text-muted-foreground">
                                  Bloom Level:
                                </span>
                                <span className="text-foreground ml-2 capitalize">
                                  {
                                    uploadResult.document.extracted_fields
                                      .bloom_level
                                  }
                                </span>
                              </div>
                            )}
                            {uploadResult.document.extracted_fields
                              .duration_min_minutes && (
                              <div>
                                <span className="font-medium text-muted-foreground">
                                  Duration:
                                </span>
                                <span className="text-foreground ml-2">
                                  {
                                    uploadResult.document.extracted_fields
                                      .duration_min_minutes
                                  }
                                  {uploadResult.document.extracted_fields
                                    .duration_max_minutes
                                    ? `-${uploadResult.document.extracted_fields.duration_max_minutes}`
                                    : ""}{" "}
                                  min
                                </span>
                              </div>
                            )}
                            {(uploadResult.document.extracted_fields
                              .mental_load as string) && (
                              <div>
                                <span className="font-medium text-muted-foreground">
                                  Mental Load:
                                </span>
                                <span className="text-foreground ml-2 capitalize">
                                  {
                                    uploadResult.document.extracted_fields
                                      .mental_load as string
                                  }
                                </span>
                              </div>
                            )}
                            {(uploadResult.document.extracted_fields
                              .physical_energy as string) && (
                              <div>
                                <span className="font-medium text-muted-foreground">
                                  Physical Energy:
                                </span>
                                <span className="text-foreground ml-2 capitalize">
                                  {
                                    uploadResult.document.extracted_fields
                                      .physical_energy as string
                                  }
                                </span>
                              </div>
                            )}
                          </div>
                          {((
                            uploadResult.document.extracted_fields
                              .topics as string[]
                          )?.length > 0 ||
                            (
                              uploadResult.document.extracted_fields
                                .resources_needed as string[]
                            )?.length > 0) && (
                            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-sm">
                              {(
                                uploadResult.document.extracted_fields
                                  .topics as string[]
                              )?.length > 0 && (
                                <div>
                                  <span className="font-medium text-muted-foreground">
                                    Topics:
                                  </span>
                                  <div className="flex flex-wrap gap-1 mt-1">
                                    {(
                                      uploadResult.document.extracted_fields
                                        .topics as string[]
                                    ).map((topic: string, index: number) => (
                                      <span
                                        key={index}
                                        className="bg-primary/10 text-primary px-2 py-1 rounded text-xs capitalize"
                                      >
                                        {topic}
                                      </span>
                                    ))}
                                  </div>
                                </div>
                              )}
                              {(
                                uploadResult.document.extracted_fields
                                  .resources_needed as string[]
                              )?.length > 0 && (
                                <div>
                                  <span className="font-medium text-muted-foreground">
                                    Resources:
                                  </span>
                                  <div className="flex flex-wrap gap-1 mt-1">
                                    {(
                                      uploadResult.document.extracted_fields
                                        .resources_needed as string[]
                                    ).map((resource: string, index: number) => (
                                      <span
                                        key={index}
                                        className="bg-secondary/20 text-secondary-foreground px-2 py-1 rounded text-xs capitalize"
                                      >
                                        {resource}
                                      </span>
                                    ))}
                                  </div>
                                </div>
                              )}
                            </div>
                          )}
                        </div>
                      </div>
                    )}

                    <div className="pt-4 border-t">
                      <Button
                        onClick={() => setShowActivityModal(true)}
                        className="w-full"
                      >
                        <Plus className="h-4 w-4 mr-2" />
                        Create Activity from PDF
                      </Button>
                    </div>
                  </div>
                )}
              </div>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Activity Creation Modal */}
      {((uploadResult?.success && uploadResult.document) ||
        uploadedDocumentId) && (
        <ActivityCreationModal
          isOpen={showActivityModal}
          onClose={() => {
            setShowActivityModal(false);
            setIsManualEntry(false);
            // Clear the uploaded document ID if user closes without creating
            if (!uploadResult?.success) {
              setUploadedDocumentId(null);
              setProcessingError(null);
            }
          }}
          pdfDocumentId={
            uploadResult?.document?.id || uploadedDocumentId || undefined
          }
          extractedFields={
            isManualEntry
              ? {
                  name: "",
                  description: "",
                  source: "",
                  age_min: undefined,
                  age_max: undefined,
                  format: "",
                  bloom_level: "",
                  duration_min_minutes: undefined,
                  duration_max_minutes: undefined,
                  mental_load: "",
                  physical_energy: "",
                  prep_time_minutes: undefined,
                  cleanup_time_minutes: undefined,
                  resources_needed: [],
                  topics: [],
                }
              : uploadResult?.document?.extracted_fields || {}
          }
          onSuccess={(activity: Activity) => {
            logger.debug("Activity created", activity, "UploadTab");
            setShowActivityModal(false);
            setUploadedDocumentId(null);
            setProcessingError(null);
            setIsManualEntry(false);
            // Navigate to the newly created activity details page
            navigate(`/activity-details/${activity.id}`, {
              state: { activity, fromBrowser: false },
            });
          }}
        />
      )}
    </div>
  );
};
