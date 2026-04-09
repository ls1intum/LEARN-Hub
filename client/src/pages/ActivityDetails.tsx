import React, { useCallback } from "react";
import { useParams, useNavigate, useLocation } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { LoadingState, SkeletonGrid } from "@/components/ui/LoadingState";
import { ErrorDisplay } from "@/components/ui/ErrorDisplay";
import { useDataFetch } from "@/hooks/useDataFetch";
import { useApi } from "@/hooks/useApi";
import {
  Brain,
  Activity as ActivityIcon,
  FileText,
  BookOpen,
  Edit3,
  ArrowLeft,
  Download,
} from "lucide-react";
import { FavouriteButton } from "@/components/favourites/FavouriteButton";
import { apiService } from "@/services/apiService";
import { useAuth } from "@/hooks/useAuth";
import { useTranslation } from "react-i18next";
import { openPdfInNewTab } from "@/utils/pdf";

interface ActivityDetailsLocationState {
  backTo?: string;
  restoreScrollY?: number;
}

export const ActivityDetails: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const location = useLocation();
  const { user } = useAuth();
  const isAdmin = user?.role === "ADMIN";
  const { t } = useTranslation();

  const navigationState = location.state as ActivityDetailsLocationState | null;
  const backTo = navigationState?.backTo;
  const restoreScrollY = navigationState?.restoreScrollY;

  const documentApi = useApi();
  const fetchActivity = useCallback(async () => {
    if (id) {
      const fetchedActivity = await apiService.getActivity(id);
      if (!fetchedActivity) {
        throw new Error("Activity not found");
      }
      return fetchedActivity;
    }

    throw new Error("No activity ID provided");
  }, [id]);

  const {
    data: activity,
    isLoading,
    error,
    refetch,
  } = useDataFetch({
    fetchFn: fetchActivity,
    enabled: !!id,
    dependencies: [fetchActivity],
  });

  const openBlobInNewTab = async (
    getBlob: () => Promise<Blob>,
    title?: string,
  ) => {
    const blob = await getBlob();
    openPdfInNewTab(blob, title);
  };

  const downloadBlob = async (
    getBlob: () => Promise<Blob>,
    filename: string,
  ) => {
    const blob = await getBlob();
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    window.URL.revokeObjectURL(url);
  };

  const handleOpenDocument = async (documentId: string, filename?: string) => {
    await documentApi.call(async () => {
      await openBlobInNewTab(
        () => apiService.downloadDocument(documentId),
        filename,
      );
    });
  };

  const handleDownloadMarkdownPdf = async (
    markdownId: string,
    title: string,
  ) => {
    await documentApi.call(async () => {
      await openBlobInNewTab(
        () => apiService.getMarkdownPdf(markdownId),
        title,
      );
    });
  };

  const handleDownloadMarkdownDocx = async (
    markdownId: string,
    markdownType: string,
  ) => {
    if (!activity?.name) return;

    await documentApi.call(async () => {
      const filename = `${activity.name || "activity"}_${markdownType}.docx`;
      await downloadBlob(
        () => apiService.getMarkdownDocx(markdownId),
        filename,
      );
    });
  };

  const handleOpenActivityPdf = async () => {
    if (!activity?.id) return;

    await documentApi.call(async () => {
      await openBlobInNewTab(
        () => apiService.downloadActivityPdf(activity.id),
        activity.name,
      );
    });
  };

  const handleOpenActivityDocx = async () => {
    if (!activity?.id) return;

    await documentApi.call(async () => {
      const filename = `${activity.name || "activity"}.docx`;
      await downloadBlob(
        () => apiService.downloadActivityDocx(activity.id),
        filename,
      );
    });
  };

  const handleBack = () => {
    if (backTo) {
      navigate(backTo, {
        replace: true,
        state:
          typeof restoreScrollY === "number" ? { restoreScrollY } : undefined,
      });
      return;
    }

    navigate("/recommendations");
  };

  const translateEnum = (
    category: string,
    value: string | undefined,
  ): string => {
    if (!value) return "";
    const key = `enums.${category}.${value}`;
    const translated = t(key);
    // If no translation found, fall back to original value
    return translated === key ? value : translated;
  };

  if (isLoading) {
    return (
      <div className="w-full">
        <LoadingState isLoading={true} fallback={<SkeletonGrid />}>
          <div className="text-center py-12">
            <p className="text-muted-foreground">
              {t("activityDetails.loading")}
            </p>
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
            <Button onClick={handleBack}>{t("activityDetails.goBack")}</Button>
          </div>
        </div>
      </div>
    );
  }

  const ageRange =
    activity.ageMin && activity.ageMax
      ? `${activity.ageMin}-${activity.ageMax}`
      : activity.ageMin
        ? `${activity.ageMin}+`
        : "";

  const durationRange =
    activity.durationMinMinutes && activity.durationMaxMinutes
      ? `${activity.durationMinMinutes}-${activity.durationMaxMinutes} ${t("common.minutes")}`
      : activity.durationMinMinutes
        ? `${activity.durationMinMinutes}+ ${t("common.minutes")}`
        : "";

  const totalTime =
    (activity.durationMinMinutes || 0) +
    (activity.prepTimeMinutes || 0) +
    (activity.cleanupTimeMinutes || 0);
  const hasDownloads =
    (activity.documents && activity.documents.length > 0) ||
    (activity.markdowns && activity.markdowns.length > 0);

  return (
    <div className="w-full py-6">
      <div className="mb-8">
        <div className="flex flex-col sm:flex-row sm:justify-between sm:items-start gap-4">
          <div>
            <div className="flex items-center gap-3">
              <Button
                variant="ghost"
                size="icon"
                onClick={handleBack}
                aria-label={t("activityDetails.goBack")}
                className="h-9 w-9 flex-shrink-0"
              >
                <ArrowLeft className="h-5 w-5" />
              </Button>
              <h2 className="text-2xl sm:text-3xl font-bold tracking-tight text-foreground">
                {t("activityDetails.title")}
              </h2>
            </div>
            <p className="text-sm sm:text-base text-muted-foreground mt-1.5 ml-12">
              {t("activityDetails.description")}
            </p>
          </div>
          <div className="flex flex-wrap gap-2">
            <FavouriteButton activityId={activity.id} size="default" />
            {isAdmin && (
              <Button
                onClick={() => navigate(`/activity-edit/${activity.id}`)}
                variant="outline"
                className="flex items-center gap-2"
              >
                <Edit3 className="h-4 w-4" />
                {t("activityDetails.edit")}
              </Button>
            )}
          </div>
        </div>
      </div>

      <div className="bg-card p-6 rounded-lg shadow-sm border border-border">
        <div className="p-2 sm:p-4">
          <h3 className="text-2xl font-bold mb-4 text-foreground">
            {activity.name}
          </h3>

          {/* Description */}
          {activity.description && (
            <div className="mb-8 p-4 bg-muted/30 rounded-lg border border-border/50">
              <h4 className="text-lg font-semibold mb-3 text-foreground">
                {t("activityDetails.descriptionLabel")}
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
                    {t("activityDetails.ageRange")}
                  </h3>
                  <p className="text-muted-foreground">{ageRange}</p>
                </div>
              )}

              {activity.format && (
                <div>
                  <h3 className="font-semibold text-card-foreground">
                    {t("activityDetails.format")}
                  </h3>
                  <p className="text-muted-foreground">
                    {translateEnum("format", activity.format)}
                  </p>
                </div>
              )}

              {activity.bloomLevel && (
                <div>
                  <h3 className="font-semibold text-card-foreground">
                    {t("activityDetails.bloomLevel")}
                  </h3>
                  <p className="text-muted-foreground">
                    {translateEnum("bloomLevel", activity.bloomLevel)}
                  </p>
                </div>
              )}

              {durationRange && (
                <div>
                  <h3 className="font-semibold text-card-foreground">
                    {t("activityDetails.duration")}
                  </h3>
                  <p className="text-muted-foreground">{durationRange}</p>
                </div>
              )}

              {totalTime > 0 && (
                <div>
                  <h3 className="font-semibold text-card-foreground">
                    {t("activityDetails.totalTime")}
                  </h3>
                  <p className="text-muted-foreground">
                    {totalTime} {t("common.minutes")}
                  </p>
                </div>
              )}
            </div>

            <div className="space-y-4">
              {(activity.mentalLoad || activity.physicalEnergy) && (
                <div>
                  <h3 className="font-semibold text-card-foreground">
                    {t("activityDetails.energyRequirements")}
                  </h3>
                  <div className="text-muted-foreground space-y-2">
                    {activity.mentalLoad && (
                      <div className="flex items-center gap-2">
                        <Brain className="h-4 w-4 text-blue-500" />
                        <span>
                          {t("activityDetails.mental")}:{" "}
                          {translateEnum("energy", activity.mentalLoad)}
                        </span>
                      </div>
                    )}
                    {activity.physicalEnergy && (
                      <div className="flex items-center gap-2">
                        <ActivityIcon className="h-4 w-4 text-orange-500" />
                        <span>
                          {t("activityDetails.physical")}:{" "}
                          {translateEnum("energy", activity.physicalEnergy)}
                        </span>
                      </div>
                    )}
                  </div>
                </div>
              )}

              {activity.resourcesNeeded &&
                activity.resourcesNeeded.length > 0 && (
                  <div>
                    <h3 className="font-semibold text-card-foreground">
                      {t("activityDetails.resourcesNeeded")}
                    </h3>
                    <ul className="text-muted-foreground list-disc list-inside">
                      {activity.resourcesNeeded.map((resource, index) => (
                        <li key={index}>
                          {translateEnum("resources", resource)}
                        </li>
                      ))}
                    </ul>
                  </div>
                )}

              {activity.source && (
                <div>
                  <h3 className="font-semibold text-card-foreground">
                    {t("activityDetails.source")}
                  </h3>
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
              <h3 className="text-lg font-semibold mb-4 text-card-foreground">
                {t("activityDetails.topics")}
              </h3>
              <div className="flex flex-wrap gap-2">
                {activity.topics.map((topic, index) => (
                  <span
                    key={index}
                    className="inline-block bg-primary text-primary-foreground text-sm font-medium px-3 py-1 rounded-full"
                  >
                    {translateEnum("topics", topic)}
                  </span>
                ))}
              </div>
            </div>
          )}

          {/* Downloads */}
          {hasDownloads && (
            <div className="rounded-lg border border-border bg-muted/20">
              <div className="border-b border-border px-4 py-3">
                <h3 className="text-lg font-semibold text-card-foreground">
                  {t("activityDetails.downloads")}
                </h3>
              </div>

              {activity.documents?.map((doc, index) => (
                <div
                  key={doc.id}
                  className={`flex flex-col gap-4 px-4 py-4 sm:flex-row sm:items-center sm:justify-between${index > 0 ? " border-t border-border" : ""}`}
                >
                  <div className="min-w-0">
                    <div className="flex items-center gap-2 text-card-foreground">
                      <FileText className="h-4 w-4 text-green-600" />
                      <p className="font-medium break-all">{doc.filename}</p>
                    </div>
                    <p className="text-sm text-muted-foreground">
                      {doc.type}
                      {Number.isFinite(doc.fileSize)
                        ? ` • ${(doc.fileSize / 1024).toFixed(1)} KB`
                        : ""}
                    </p>
                  </div>
                  <div className="flex items-center gap-2 sm:shrink-0">
                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      disabled={documentApi.isLoading}
                      onClick={() => handleOpenDocument(doc.id, doc.filename)}
                      title={t("activityDetails.downloadPdf")}
                      className="flex items-center gap-1.5"
                    >
                      <Download className="h-4 w-4 text-red-600" />
                      <span>{t("activityDetails.downloadPdf")}</span>
                    </Button>
                  </div>
                </div>
              ))}

              {activity.markdowns?.map((md) => (
                <div
                  key={md.id}
                  className="flex flex-col gap-4 border-t border-border px-4 py-4 sm:flex-row sm:items-center sm:justify-between"
                >
                  <div className="min-w-0">
                    <div className="flex items-center gap-2 text-card-foreground">
                      <BookOpen className="h-4 w-4 text-blue-600" />
                      <p className="font-medium break-all">
                        {activity.name} {md.type}
                      </p>
                    </div>
                    <p className="text-sm text-muted-foreground">{md.type}</p>
                  </div>
                  <div className="flex items-center gap-2 sm:shrink-0">
                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      disabled={documentApi.isLoading}
                      onClick={() =>
                        handleDownloadMarkdownPdf(
                          md.id,
                          [activity.name, md.type].filter(Boolean).join(" "),
                        )
                      }
                      title="Download as PDF"
                      className="flex items-center gap-1.5"
                    >
                      <Download className="h-4 w-4 text-red-600" />
                      <span>{t("activityDetails.downloadPdf")}</span>
                    </Button>
                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      disabled={documentApi.isLoading}
                      onClick={() => handleDownloadMarkdownDocx(md.id, md.type)}
                      title="Download as Word document"
                      className="flex items-center gap-1.5"
                    >
                      <Download className="h-4 w-4 text-blue-600" />
                      <span>{t("activityDetails.downloadDocx")}</span>
                    </Button>
                  </div>
                </div>
              ))}

              {/* Download Activity (combined) */}
              {activity.markdowns && activity.markdowns.length > 0 && (
                <div className="flex flex-col gap-4 border-t border-border px-4 py-4 sm:flex-row sm:items-center sm:justify-between bg-muted/30">
                  <div className="min-w-0">
                    <div className="flex items-center gap-2 text-card-foreground">
                      <Download className="h-4 w-4 text-primary" />
                      <p className="font-medium">
                        {t("activityDetails.downloadActivity")}
                      </p>
                    </div>
                    <p className="text-sm text-muted-foreground">
                      {t("activityDetails.combinedDocument")}
                    </p>
                  </div>
                  <div className="flex items-center gap-2 sm:shrink-0">
                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      disabled={documentApi.isLoading}
                      onClick={handleOpenActivityPdf}
                      title="Open combined activity as PDF"
                      className="flex items-center gap-1.5"
                    >
                      <Download className="h-4 w-4 text-red-600" />
                      <span>{t("activityDetails.downloadPdf")}</span>
                    </Button>
                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      disabled={documentApi.isLoading}
                      onClick={handleOpenActivityDocx}
                      title="Download combined activity as Word document"
                      className="flex items-center gap-1.5"
                    >
                      <Download className="h-4 w-4 text-blue-600" />
                      <span>{t("activityDetails.downloadDocx")}</span>
                    </Button>
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
