import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { logger } from "@/services/logger";
import {
  CheckCircle,
  AlertCircle,
  ExternalLink,
  RefreshCw,
  Edit,
} from "lucide-react";
import { FileUploadArea } from "@/components/ui/FileUploadArea";
import { SelectedFileInfo } from "@/components/ui/SelectedFileInfo";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Upload, Loader2 } from "lucide-react";
import { apiService } from "@/services/apiService";
import { ActivityCreationModal } from "@/components/ActivityCreationModal";
import type { FormFieldData } from "@/types/api";
import type { Activity } from "@/types/activity";

interface UploadResultData {
  success: boolean;
  message: string;
  activity?: FormFieldData & {
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
  document_id?: number;
  extraction_confidence?: number;
  extraction_quality?: string;
}

export const ActivityUploader: React.FC = () => {
  const navigate = useNavigate();
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [isUploading, setIsUploading] = useState(false);
  const [uploadResult, setUploadResult] = useState<UploadResultData | null>(
    null,
  );
  const [dragActive, setDragActive] = useState(false);
  const [loadingMessage, setLoadingMessage] = useState<string>(
    "Creating Activity...",
  );
  const [showManualEntryModal, setShowManualEntryModal] = useState(false);
  const [extractedData, setExtractedData] = useState<
    Record<string, unknown> | undefined
  >(undefined);
  const [pdfDocumentId, setPdfDocumentId] = useState<number | undefined>(
    undefined,
  );

  // Cycle through loading messages to keep user informed
  useEffect(() => {
    if (!isUploading) return;

    const messages = [
      "Uploading PDF...",
      "Extracting text from PDF...",
      "Processing with AI (this may take ~10 seconds)...",
      "Analyzing activity data...",
      "Almost done...",
    ];

    let currentIndex = 0;
    setLoadingMessage(messages[0]);

    const interval = setInterval(() => {
      currentIndex = (currentIndex + 1) % messages.length;
      setLoadingMessage(messages[currentIndex]);
    }, 2500); // Change message every 2.5 seconds

    return () => clearInterval(interval);
  }, [isUploading]);

  const validateFile = (file: File) => {
    // Check file type
    if (file.type !== "application/pdf") {
      return { isValid: false, message: "Please select a PDF file" };
    }

    // Check if file is empty
    if (file.size === 0) {
      return { isValid: false, message: "The selected file is empty" };
    }

    // Check file size (10MB limit)
    if (file.size > 10 * 1024 * 1024) {
      return { isValid: false, message: "File size must be less than 10MB" };
    }

    return { isValid: true };
  };

  const handleFileSelect = (file: File) => {
    const validation = validateFile(file);

    if (!validation.isValid) {
      setUploadResult({
        success: false,
        message: validation.message || "Invalid file",
      });
      return;
    }

    setSelectedFile(file);
    setUploadResult(null);
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
    setExtractedData(undefined);
    setPdfDocumentId(undefined);

    try {
      const response = await apiService.uploadAndCreateActivity(selectedFile);

      const result = {
        success: true,
        message:
          "Activity created successfully from PDF! Redirecting to activity details...",
        activity: response.activity,
        document_id: response.document_id,
        extraction_confidence: response.extraction_confidence,
        extraction_quality: response.extraction_quality,
      };

      setUploadResult(result);

      if (result.success) {
        setSelectedFile(null);
        // Reset file input
        const fileInput = document.getElementById(
          "pdf-file",
        ) as HTMLInputElement;
        if (fileInput) fileInput.value = "";

        // Navigate to activity details if we have an activity ID
        const activityId = result.activity?.id;
        if (activityId) {
          navigate(`/activity-details/${activityId}`);
        } else {
          logger.error(
            "No activity ID found in response",
            result,
            "ActivityUploader",
          );
        }
      }
    } catch (error) {
      logger.error("Upload error", error, "ActivityUploader");

      // Extract any partial data from the error if available
      const errorData = error as {
        response?: {
          data?: { activity?: FormFieldData; document_id?: number };
        };
      };
      if (errorData.response?.data?.activity) {
        setExtractedData(errorData.response.data.activity);
      }
      if (errorData.response?.data?.document_id) {
        setPdfDocumentId(errorData.response.data.document_id);
      }

      const errorMessage =
        error instanceof Error ? error.message : "Unknown error occurred";

      setUploadResult({
        success: false,
        message: `Failed to create activity from PDF: ${errorMessage}`,
      });
    } finally {
      setIsUploading(false);
    }
  };

  const handleManualEntry = () => {
    setShowManualEntryModal(true);
  };

  const handleManualEntrySuccess = (activity: Activity) => {
    setShowManualEntryModal(false);
    setSelectedFile(null);
    setUploadResult(null);
    navigate(`/activity-details/${activity.id}`);
  };

  return (
    <div className="w-full max-w-4xl mx-auto space-y-6">
      {/* Header */}
      <div className="text-center">
        <h2 className="text-2xl font-bold text-foreground mb-2">
          Upload Activity PDF
        </h2>
        <p className="text-muted-foreground">
          Upload a PDF file to automatically extract and create an activity
        </p>
      </div>

      {/* Upload Card */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Upload className="h-5 w-5" />
            PDF Upload & Activity Creation
          </CardTitle>
          <CardDescription>
            Select a PDF file containing activity information. The system will
            automatically extract data and create the activity.
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <FileUploadArea
            onFileSelect={handleFileSelect}
            dragActive={dragActive}
            onDrag={handleDrag}
            onDrop={handleDrop}
          />

          {/* Selected File Info */}
          {selectedFile && <SelectedFileInfo file={selectedFile} />}

          {/* Upload Button */}
          <Button
            onClick={handleUpload}
            disabled={!selectedFile || isUploading}
            className="w-full"
          >
            {isUploading ? (
              <>
                <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                {loadingMessage}
              </>
            ) : (
              <>
                <Upload className="h-4 w-4 mr-2" />
                Upload & Create Activity
              </>
            )}
          </Button>
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

                {/* Error actions: Retry and Manual Entry */}
                {!uploadResult.success && (
                  <div className="mt-4 flex flex-col sm:flex-row gap-3">
                    <Button
                      onClick={handleUpload}
                      variant="default"
                      className="flex-1"
                    >
                      <RefreshCw className="h-4 w-4 mr-2" />
                      Retry Upload
                    </Button>
                    <Button
                      onClick={handleManualEntry}
                      variant="outline"
                      className="flex-1"
                    >
                      <Edit className="h-4 w-4 mr-2" />
                      Enter Manually
                    </Button>
                  </div>
                )}

                {uploadResult.success && uploadResult.activity && (
                  <div className="mt-4 space-y-4">
                    <h4 className="font-medium text-foreground">
                      Activity Created Successfully:
                    </h4>

                    {/* Activity details in a compact layout */}
                    <div className="bg-muted/30 rounded-lg p-4 space-y-3">
                      <div>
                        <span className="font-medium text-muted-foreground text-sm">
                          Activity Name:
                        </span>
                        <p className="text-foreground font-medium mt-1">
                          {uploadResult.activity.name as string}
                        </p>
                      </div>
                      {uploadResult.activity.description && (
                        <div>
                          <span className="font-medium text-muted-foreground text-sm">
                            Description:
                          </span>
                          <p className="text-foreground mt-1 text-sm leading-relaxed">
                            {uploadResult.activity.description as string}
                          </p>
                        </div>
                      )}
                      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3 text-sm">
                        {uploadResult.activity.age_min &&
                          uploadResult.activity.age_max && (
                            <div>
                              <span className="font-medium text-muted-foreground">
                                Age Range:
                              </span>
                              <span className="text-foreground ml-2">
                                {uploadResult.activity.age_min}-
                                {uploadResult.activity.age_max} years
                              </span>
                            </div>
                          )}
                        {uploadResult.activity.format && (
                          <div>
                            <span className="font-medium text-muted-foreground">
                              Format:
                            </span>
                            <span className="text-foreground ml-2 capitalize">
                              {uploadResult.activity.format as string}
                            </span>
                          </div>
                        )}
                        {uploadResult.activity.bloom_level && (
                          <div>
                            <span className="font-medium text-muted-foreground">
                              Bloom Level:
                            </span>
                            <span className="text-foreground ml-2 capitalize">
                              {uploadResult.activity.bloom_level as string}
                            </span>
                          </div>
                        )}
                        {uploadResult.activity.duration_min_minutes && (
                          <div>
                            <span className="font-medium text-muted-foreground">
                              Duration:
                            </span>
                            <span className="text-foreground ml-2">
                              {uploadResult.activity.duration_min_minutes}
                              {uploadResult.activity.duration_max_minutes
                                ? `-${uploadResult.activity.duration_max_minutes}`
                                : ""}{" "}
                              min
                            </span>
                          </div>
                        )}
                        {uploadResult.extraction_confidence !== undefined && (
                          <div>
                            <span className="font-medium text-muted-foreground">
                              Confidence:
                            </span>
                            <span className="text-foreground ml-2">
                              {Math.round(
                                uploadResult.extraction_confidence * 100,
                              )}
                              %
                            </span>
                          </div>
                        )}
                        {uploadResult.extraction_quality && (
                          <div>
                            <span className="font-medium text-muted-foreground">
                              Quality:
                            </span>
                            <span className="text-foreground ml-2 capitalize">
                              {uploadResult.extraction_quality}
                            </span>
                          </div>
                        )}
                      </div>
                    </div>

                    <div className="pt-4 border-t space-y-2">
                      <Button
                        onClick={() => {
                          const activityId = uploadResult.activity?.id;
                          if (activityId) {
                            navigate(`/activity-details/${activityId}`);
                          }
                        }}
                        className="w-full"
                      >
                        <ExternalLink className="h-4 w-4 mr-2" />
                        View Activity Details
                      </Button>
                      <Button
                        onClick={() => window.location.reload()}
                        variant="outline"
                        className="w-full"
                      >
                        Upload Another Activity
                      </Button>
                      <p className="text-xs text-muted-foreground text-center mt-2">
                        You will be automatically redirected to the activity
                        details page in a few seconds.
                      </p>
                    </div>
                  </div>
                )}
              </div>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Manual Entry Modal */}
      <ActivityCreationModal
        isOpen={showManualEntryModal}
        onClose={() => setShowManualEntryModal(false)}
        extractedFields={extractedData}
        pdfDocumentId={pdfDocumentId}
        onSuccess={handleManualEntrySuccess}
      />
    </div>
  );
};
