import React, { useCallback, useState } from "react";
import { useParams, useNavigate, useLocation } from "react-router-dom";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { LoadingState, SkeletonGrid } from "@/components/ui/LoadingState";
import { ErrorDisplay } from "@/components/ui/ErrorDisplay";
import { useDataFetch } from "@/hooks/useDataFetch";
import { useApi } from "@/hooks/useApi";
import { Brain, Activity as ActivityIcon, FileText, Eye } from "lucide-react";
import { FavouriteButton } from "@/components/favourites/FavouriteButton";
import { apiService } from "@/services/apiService";
import type { Activity } from "@/types/activity";

// Note: PDFInfo interface is defined but not currently used in the refactored version
// interface PDFInfo { ... }

export const ActivityDetails: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const location = useLocation();
  const [isPdfModalOpen, setIsPdfModalOpen] = useState(false);
  const [pdfUrl, setPdfUrl] = useState<string | null>(null);
  const [isLoadingPdf] = useState(false);

  // Get activity data from navigation state or fetch from API
  const stateActivity = location.state?.activity as Activity | undefined;
  const fromBrowser = location.state?.fromBrowser as boolean | undefined;

  // API hooks for data fetching
  const downloadApi = useApi();
  const pdfApi = useApi();

  // Data fetching for activity details
  const fetchActivity = useCallback(async () => {
    if (stateActivity) {
      return stateActivity;
    }
    if (id) {
      const fetchedActivity = await apiService.getActivity(parseInt(id));
      if (!fetchedActivity) {
        throw new Error("Activity not found");
      }
      return fetchedActivity;
    }
    throw new Error("No activity ID provided");
  }, [stateActivity, id]);

  const { data: activity, isLoading, error, refetch } = useDataFetch({
    fetchFn: fetchActivity,
    enabled: !!(stateActivity || id),
    dependencies: [stateActivity, id],
  });

  // Data fetching for PDF info
  const fetchPdfInfo = useCallback(async () => {
    if (!activity?.document_id) return null;
    try {
      const response = await apiService.getDocumentInfo(activity.document_id);
      if (response) {
        return {
          id: response.id,
          filename: response.filename,
          file_size: response.file_size,
          extracted_fields: {},
          created_at: response.created_at,
        };
      }
    } catch {
      // Gracefully handle missing/404 document by treating as no PDF
      return null;
    }
    return null;
  }, [activity?.document_id]);

  const { data: pdfInfo } = useDataFetch({
    fetchFn: fetchPdfInfo,
    enabled: !!activity?.document_id,
    dependencies: [activity?.document_id],
  });

  const handleDownloadPDF = async () => {
    if (!activity?.id) return;

    await downloadApi.call(async () => {
      const blob = await apiService.getActivityPdf(activity.id as number);
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = pdfInfo?.filename || `activity_${id}.pdf`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
    });
  };

  const handlePreviewPDF = async () => {
    if (!activity?.id) return;

    setIsPdfModalOpen(true);

    await pdfApi.call(async () => {
      const pdfBlob = await apiService.getActivityPdf(activity.id as number);
      const url = URL.createObjectURL(pdfBlob);
      setPdfUrl(url);
    });
  };

  const closePdfModal = () => {
    setIsPdfModalOpen(false);
    if (pdfUrl) {
      URL.revokeObjectURL(pdfUrl);
      setPdfUrl(null);
    }
  };

  const handleBack = () => {
    if (fromBrowser) {
      navigate(-1); // Go back to library when navigated from there
    } else {
      navigate("/recommendations"); // Go to recommendations form otherwise
    }
  };

  if (isLoading) {
    return (
      <div className="w-full">
        <LoadingState isLoading={true} fallback={<SkeletonGrid />}>
          <div className="text-center py-12">
            <p className="text-muted-foreground">Loading activity details...</p>
          </div>
        </LoadingState>
      </div>
    );
  }

  if (error || !activity) {
    return (
      <div className="w-full">
        <div className="text-center py-12">
          <ErrorDisplay
            error={error || "Activity not found"}
            onRetry={refetch}
          />
          <div className="mt-4">
            <Button onClick={handleBack}>Go Back</Button>
          </div>
        </div>
      </div>
    );
  }

  const ageRange =
    activity.age_min && activity.age_max
      ? `${activity.age_min}-${activity.age_max}`
      : activity.age_min
        ? `${activity.age_min}+`
        : "";

  const durationRange =
    activity.duration_min_minutes && activity.duration_max_minutes
      ? `${activity.duration_min_minutes}-${activity.duration_max_minutes} minutes`
      : activity.duration_min_minutes
        ? `${activity.duration_min_minutes}+ minutes`
        : "";

  const totalTime =
    (activity.duration_min_minutes || 0) +
    (activity.prep_time_minutes || 0) +
    (activity.cleanup_time_minutes || 0);

  return (
    <div className="w-full max-w-4xl mx-auto px-4">
      <div className="mb-8">
        <div className="flex justify-between items-center">
          <div>
            <h2 className="text-2xl font-bold text-foreground mb-1 text-center sm:text-left">
              Activity Details
            </h2>
            <p className="text-base text-muted-foreground text-center sm:text-left">
              Detailed information about this educational activity.
            </p>
          </div>
          <div className="flex gap-2 sm:gap-4">
            <FavouriteButton activityId={activity.id} size="default" />
            <Button
              onClick={handlePreviewPDF}
              disabled={pdfApi.isLoading}
              variant="outline"
              className="flex items-center gap-2"
            >
              <Eye className="h-4 w-4" />
              {pdfApi.isLoading ? "Loading..." : "Preview PDF"}
            </Button>
            <Button
              onClick={handleDownloadPDF}
              disabled={downloadApi.isLoading}
              className="bg-green-500 hover:bg-green-700 flex items-center gap-2"
            >
              <FileText className="h-4 w-4" />
              {downloadApi.isLoading ? "Downloading..." : "Download PDF"}
            </Button>
            <Button onClick={handleBack} variant="outline">
              {fromBrowser ? "Back to Library" : "Back to Form"}
            </Button>
          </div>
        </div>
      </div>

      <div className="bg-card p-6 rounded-lg shadow-sm border border-border">
        <div className="p-2 sm:p-4">
          <h3 className="text-3xl font-bold mb-4 text-foreground">
            {activity.name}
          </h3>

          {/* Description */}
          {activity.description && (
            <div className="mb-8 p-4 bg-muted/30 rounded-lg border border-border/50">
              <h4 className="text-lg font-semibold mb-3 text-foreground">
                Description
              </h4>
              <p className="text-muted-foreground leading-relaxed whitespace-pre-wrap">
                {activity.description}
              </p>
            </div>
          )}

          {/* Basic Information */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-8">
            <div className="space-y-4">
              {ageRange && (
                <div>
                  <h3 className="font-semibold text-card-foreground">
                    Age Range
                  </h3>
                  <p className="text-muted-foreground">{ageRange}</p>
                </div>
              )}

              {activity.format && (
                <div>
                  <h3 className="font-semibold text-card-foreground">Format</h3>
                  <p className="text-muted-foreground">{activity.format}</p>
                </div>
              )}

              {activity.bloom_level && (
                <div>
                  <h3 className="font-semibold text-card-foreground">
                    Bloom's Taxonomy Level
                  </h3>
                  <p className="text-muted-foreground">
                    {activity.bloom_level}
                  </p>
                </div>
              )}

              {durationRange && (
                <div>
                  <h3 className="font-semibold text-card-foreground">
                    Duration
                  </h3>
                  <p className="text-muted-foreground">{durationRange}</p>
                </div>
              )}

              {totalTime > 0 && (
                <div>
                  <h3 className="font-semibold text-card-foreground">
                    Total Time (including prep & cleanup)
                  </h3>
                  <p className="text-muted-foreground">{totalTime} minutes</p>
                </div>
              )}
            </div>

            <div className="space-y-4">
              {(activity.mental_load || activity.physical_energy) && (
                <div>
                  <h3 className="font-semibold text-card-foreground">
                    Energy Requirements
                  </h3>
                  <div className="text-muted-foreground space-y-2">
                    {activity.mental_load && (
                      <div className="flex items-center gap-2">
                        <Brain className="h-4 w-4 text-blue-500" />
                        <span>Mental: {activity.mental_load}</span>
                      </div>
                    )}
                    {activity.physical_energy && (
                      <div className="flex items-center gap-2">
                        <ActivityIcon className="h-4 w-4 text-orange-500" />
                        <span>Physical: {activity.physical_energy}</span>
                      </div>
                    )}
                  </div>
                </div>
              )}

              {activity.resources_needed &&
                activity.resources_needed.length > 0 && (
                  <div>
                    <h3 className="font-semibold text-card-foreground">
                      Resources Needed
                    </h3>
                    <ul className="text-muted-foreground list-disc list-inside">
                      {activity.resources_needed.map((resource, index) => (
                        <li key={index}>{resource}</li>
                      ))}
                    </ul>
                  </div>
                )}

              {activity.source && (
                <div>
                  <h3 className="font-semibold text-card-foreground">Source</h3>
                  {(() => {
                    try {
                      const url = new URL(activity.source);
                      return (
                        <a
                          href={activity.source}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="text-primary hover:underline"
                        >
                          {url.hostname}
                        </a>
                      );
                    } catch {
                      return (
                        <p className="text-muted-foreground">
                          {activity.source}
                        </p>
                      );
                    }
                  })()}
                </div>
              )}
            </div>
          </div>

          {/* Topics */}
          {activity.topics && activity.topics.length > 0 && (
            <div className="mb-8">
              <h3 className="text-xl font-semibold mb-4 text-card-foreground">
                Topics
              </h3>
              <div className="flex flex-wrap gap-2">
                {activity.topics.map((topic, index) => (
                  <span
                    key={index}
                    className="inline-block bg-primary text-primary-foreground text-sm font-medium px-3 py-1 rounded-full"
                  >
                    {topic}
                  </span>
                ))}
              </div>
            </div>
          )}

          {/* PDF Information */}
          <div className="mb-8 p-4 rounded-lg border border-success/20 bg-success/5">
            <h3 className="text-lg font-semibold mb-2 text-success">
              PDF Available
            </h3>
            {pdfInfo ? (
              <div className="text-foreground text-sm sm:text-base">
                <p>
                  <span className="font-medium">Filename:</span> {pdfInfo.filename}
                </p>
                <p>
                  <span className="font-medium">Size:</span> {(pdfInfo.file_size / 1024).toFixed(1)} KB
                </p>
                <p>
                  <span className="font-medium">Uploaded:</span> {new Date(pdfInfo.created_at).toLocaleDateString()}
                </p>
              </div>
            ) : (
              <p className="text-muted-foreground text-sm sm:text-base">
                A PDF is available for this activity. Details are currently unavailable, but you can preview or download it above.
              </p>
            )}
          </div>
        </div>
      </div>

      {/* PDF Preview Dialog */}
      <Dialog
        open={isPdfModalOpen}
        onOpenChange={(open) => {
          if (!open) {
            closePdfModal();
          } else {
            setIsPdfModalOpen(true);
          }
        }}
      >
        <DialogContent className="max-w-4xl max-h-[90vh]">
          <DialogHeader>
            <DialogTitle>Preview: {activity.name}</DialogTitle>
          </DialogHeader>
          <div className="w-full h-[70vh] flex items-center justify-center">
            {isLoadingPdf ? (
              <div className="flex flex-col items-center gap-4">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
                <p className="text-muted-foreground">Loading PDF...</p>
              </div>
            ) : pdfUrl ? (
              <iframe
                src={pdfUrl}
                className="w-full h-full border-0 rounded-lg"
                title={`PDF Preview: ${activity.name}`}
              />
            ) : (
              <div className="text-center text-muted-foreground">
                <FileText className="h-12 w-12 mx-auto mb-4 opacity-50" />
                <p>Failed to load PDF preview</p>
              </div>
            )}
          </div>
        </DialogContent>
      </Dialog>
    </div>
  );
};
