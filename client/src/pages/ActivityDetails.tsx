import React, { useCallback, useEffect, useRef, useState } from "react";
import { useParams, useNavigate, useLocation } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { ErrorDisplay } from "@/components/ui/ErrorDisplay";
import { DeleteActivityDialog } from "@/components/activities/DeleteActivityDialog";
import { Breadcrumb, type BreadcrumbItem } from "@/components/ui/Breadcrumb";
import { useDataFetch } from "@/hooks/useDataFetch";
import { useApi } from "@/hooks/useApi";
import {
  Brain,
  Activity as ActivityIcon,
  FileText,
  Clock,
  Users,
  Monitor,
  BookOpen,
  Edit3,
  Download,
  Trash2,
  Loader2,
  Send,
  MoreVertical,
} from "lucide-react";
import { FavouriteButton } from "@/components/favourites/FavouriteButton";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { apiService } from "@/services/apiService";
import { useAuth } from "@/hooks/useAuth";
import { useTranslation } from "react-i18next";
import { openPdfInNewTab } from "@/utils/pdf";
import {
  getActivityBackTarget,
  type ActivityNavigationState,
} from "@/utils/activityNavigation";

const BLOOM_ORDER = [
  "remember",
  "understand",
  "apply",
  "analyze",
  "evaluate",
  "create",
] as const;

const ActivityDetailsSkeleton: React.FC = () => (
  <div className="py-6 space-y-6">
    <div className="rounded-xl bg-primary/5 border border-primary/10 px-5 py-5 space-y-4">
      <div className="flex items-center justify-between gap-3">
        <Skeleton className="h-5 w-24" />
        <div className="flex items-center gap-2">
          <Skeleton className="h-8 w-8 rounded" />
          <Skeleton className="h-8 w-20 rounded hidden sm:block" />
          <Skeleton className="h-8 w-20 rounded hidden sm:block" />
        </div>
      </div>
      <div className="space-y-2">
        <Skeleton className="h-8 w-4/5 max-w-2xl" />
        <Skeleton className="h-8 w-1/2 max-w-md" />
      </div>
      <div className="flex flex-wrap gap-1.5">
        <Skeleton className="h-5 w-20 rounded-full" />
        <Skeleton className="h-5 w-24 rounded-full" />
        <Skeleton className="h-5 w-16 rounded-full" />
      </div>
      <div className="flex flex-wrap gap-2">
        <Skeleton className="h-6 w-24 rounded-full" />
        <Skeleton className="h-6 w-20 rounded-full" />
        <Skeleton className="h-6 w-28 rounded-full" />
      </div>
    </div>

    <div className="grid grid-cols-1 lg:grid-cols-[1fr_256px] gap-6">
      <div className="space-y-6 min-w-0">
        <section className="space-y-2">
          <Skeleton className="h-5 w-32" />
          <div className="space-y-2">
            <Skeleton className="h-4 w-full" />
            <Skeleton className="h-4 w-11/12" />
            <Skeleton className="h-4 w-2/3" />
          </div>
        </section>

        <section className="space-y-3">
          <Skeleton className="h-5 w-28" />
          <div className="flex flex-wrap gap-2">
            {Array.from({ length: 6 }).map((_, i) => (
              <Skeleton key={i} className="h-7 w-24 rounded-full" />
            ))}
          </div>
        </section>

        <section className="space-y-3">
          <Skeleton className="h-5 w-24" />
          <div className="border border-border rounded-lg overflow-hidden divide-y divide-border">
            {Array.from({ length: 3 }).map((_, i) => (
              <div key={i} className="flex items-center gap-3 px-4 py-3">
                <Skeleton className="h-4 w-4 rounded" />
                <div className="flex-1 space-y-1.5">
                  <Skeleton className="h-4 w-2/3" />
                  <Skeleton className="h-3 w-24" />
                </div>
                <Skeleton className="h-7 w-14 rounded" />
                <Skeleton className="h-7 w-14 rounded hidden sm:block" />
              </div>
            ))}
          </div>
        </section>
      </div>

      <aside className="space-y-2.5">
        {Array.from({ length: 5 }).map((_, i) => (
          <div key={i} className="border border-border rounded-lg px-4 py-3">
            <Skeleton className="h-3 w-20 mb-2" />
            <Skeleton className="h-4 w-28" />
          </div>
        ))}
      </aside>
    </div>
  </div>
);

export const ActivityDetails: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const location = useLocation();
  const { user } = useAuth();
  const isAdmin = user?.role === "ADMIN";
  const { t } = useTranslation();

  const navigationState = location.state as ActivityNavigationState | null;
  const backTo = getActivityBackTarget(navigationState?.backTo);
  const restoreScrollY = navigationState?.restoreScrollY;

  const documentApi = useApi();
  const docxApi = useApi();
  const deleteApi = useApi();
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [docxLoadingId, setDocxLoadingId] = useState<string | null>(null);
  const [isPublishing, setIsPublishing] = useState(false);

  const docxWaitingQuotes = t("activityDetails.docxWaitingQuotes", {
    returnObjects: true,
  }) as string[];
  const [quoteIndex, setQuoteIndex] = useState(0);
  const quoteIntervalRef = useRef<ReturnType<typeof setInterval> | null>(null);

  useEffect(() => {
    if (docxLoadingId !== null) {
      setQuoteIndex(Math.floor(Math.random() * docxWaitingQuotes.length));
      quoteIntervalRef.current = setInterval(() => {
        setQuoteIndex((i) => (i + 1) % docxWaitingQuotes.length);
      }, 2500);
    } else {
      if (quoteIntervalRef.current) {
        clearInterval(quoteIntervalRef.current);
        quoteIntervalRef.current = null;
      }
    }
    return () => {
      if (quoteIntervalRef.current) clearInterval(quoteIntervalRef.current);
    };
  }, [docxLoadingId, docxWaitingQuotes.length]);

  const fetchCapabilities = useCallback(
    () => apiService.getServerCapabilities(),
    [],
  );
  const { data: serverCapabilities } = useDataFetch({
    fetchFn: fetchCapabilities,
  });
  const docxAvailable = serverCapabilities?.docxAvailable ?? false;

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

  const handleDownloadMarkdownPdf = (markdownId: string) => {
    window.open(`/api/markdowns/${markdownId}/pdf`, "_blank");
  };

  const handleDownloadMarkdownDocx = async (
    markdownId: string,
    markdownType: string,
  ) => {
    if (!activity?.name || docxLoadingId) return;
    setDocxLoadingId(markdownId);
    try {
      await docxApi.call(async () => {
        const filename = `${activity.name || "activity"}_${markdownType}.docx`;
        await downloadBlob(
          () => apiService.getMarkdownDocx(markdownId),
          filename,
        );
      });
    } finally {
      setDocxLoadingId(null);
    }
  };

  const handleOpenActivityPdf = () => {
    if (!activity?.id) return;
    window.open(`/api/activities/${activity.id}/pdf`, "_blank");
  };

  const handleOpenActivityDocx = async () => {
    if (!activity?.id || docxLoadingId) return;
    setDocxLoadingId("activity");
    try {
      await docxApi.call(async () => {
        const filename = `${activity.name || "activity"}.docx`;
        await downloadBlob(
          () => apiService.downloadActivityDocx(activity.id),
          filename,
        );
      });
    } finally {
      setDocxLoadingId(null);
    }
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

  const handlePublishDraft = async () => {
    if (!activity?.id) return;
    setIsPublishing(true);
    try {
      await apiService.publishActivity(activity.id);
      navigate("/drafts", { replace: true });
    } catch (err) {
      alert(
        err instanceof Error ? err.message : "Fehler beim Veröffentlichen.",
      );
    } finally {
      setIsPublishing(false);
    }
  };

  const handleDeleteActivity = async () => {
    if (!activity?.id) return;

    await deleteApi.call(
      () => apiService.deleteActivity(activity.id),
      () => {
        setDeleteDialogOpen(false);
        navigate(backTo || "/library", {
          replace: true,
          state:
            typeof restoreScrollY === "number" ? { restoreScrollY } : undefined,
        });
      },
    );
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
    return <ActivityDetailsSkeleton />;
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
      ? activity.ageMin === activity.ageMax
        ? `${activity.ageMin}`
        : `${activity.ageMin}-${activity.ageMax}`
      : activity.ageMin
        ? `${activity.ageMin}+`
        : "";

  const durationBase =
    activity.durationMinMinutes && activity.durationMaxMinutes
      ? activity.durationMinMinutes === activity.durationMaxMinutes
        ? `${activity.durationMinMinutes}`
        : `${activity.durationMinMinutes}-${activity.durationMaxMinutes}`
      : activity.durationMinMinutes
        ? `${activity.durationMinMinutes}+`
        : "";

  const extraTime =
    (activity.prepTimeMinutes || 0) + (activity.cleanupTimeMinutes || 0);

  const durationChipText = durationBase
    ? `${durationBase}${extraTime > 0 ? ` (+${extraTime})` : ""} ${t("common.minutes")}`
    : "";
  const hasDownloads =
    (activity.documents && activity.documents.length > 0) ||
    (activity.markdowns && activity.markdowns.length > 0);

  const bloomKey = activity.bloomLevel?.toLowerCase() ?? "";
  const bloomIndex = BLOOM_ORDER.indexOf(
    bloomKey as (typeof BLOOM_ORDER)[number],
  );

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
    typeof restoreScrollY === "number" ? { restoreScrollY } : undefined;

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
  breadcrumbItems.push({ label: activity.name });

  return (
    <div className="py-6 space-y-6">
      <Breadcrumb items={breadcrumbItems} />

      {/* Hero header */}
      <div className="rounded-xl bg-primary/5 border border-primary/10 px-5 py-5 space-y-4">
        {/* Title + actions */}
        <div className="space-y-3">
          <div className="flex items-start justify-between gap-3">
            <h1 className="text-2xl sm:text-3xl font-bold tracking-tight text-foreground">
              {activity.name}
            </h1>
            <div className="flex items-center gap-2 shrink-0">
              <FavouriteButton
                activityId={activity.id}
                size="sm"
                initialIsFavourited={activity.isFavourited ?? false}
              />
              {isAdmin && (
                <>
                  {source === "drafts" && activity.status === "DRAFT" && (
                    <Button
                      size="sm"
                      className="hidden sm:flex h-8 gap-1.5"
                      onClick={() => void handlePublishDraft()}
                      disabled={isPublishing}
                    >
                      {isPublishing ? (
                        <Loader2 className="h-3.5 w-3.5 animate-spin" />
                      ) : (
                        <Send className="h-3.5 w-3.5" />
                      )}
                      Veröffentlichen
                    </Button>
                  )}
                  <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                      <Button
                        type="button"
                        size="icon"
                        variant="ghost"
                        className="h-8 w-8 text-muted-foreground"
                        aria-label="Weitere Aktionen"
                      >
                        <MoreVertical className="h-4 w-4" />
                      </Button>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent align="end">
                      <DropdownMenuItem
                        onClick={() =>
                          navigate(`${location.pathname}/edit`, {
                            state: {
                              backTo,
                              restoreScrollY,
                              detailPath: location.pathname,
                            } satisfies ActivityNavigationState,
                          })
                        }
                      >
                        <Edit3 />
                        {t("activityDetails.edit")}
                      </DropdownMenuItem>
                      <DropdownMenuSeparator />
                      <DropdownMenuItem
                        onClick={() => setDeleteDialogOpen(true)}
                        className="text-destructive focus:text-destructive"
                      >
                        <Trash2 />
                        {t("activityDetails.delete")}
                      </DropdownMenuItem>
                    </DropdownMenuContent>
                  </DropdownMenu>
                </>
              )}
            </div>
          </div>

          {/* Publish button: full width below headline on mobile */}
          {isAdmin && source === "drafts" && activity.status === "DRAFT" && (
            <Button
              className="w-full sm:hidden gap-1.5"
              onClick={() => void handlePublishDraft()}
              disabled={isPublishing}
            >
              {isPublishing ? (
                <Loader2 className="h-4 w-4 animate-spin" />
              ) : (
                <Send className="h-4 w-4" />
              )}
              Veröffentlichen
            </Button>
          )}
        </div>

        {/* Meta chips */}
        <div className="flex flex-wrap gap-2 text-xs text-muted-foreground">
          {durationChipText && (
            <span className="flex items-center gap-1 border border-border/70 rounded-full px-2.5 py-1 bg-background/60">
              <Clock className="h-3 w-3" />
              {durationChipText}
            </span>
          )}
          {ageRange && (
            <span className="flex items-center gap-1 border border-border/70 rounded-full px-2.5 py-1 bg-background/60">
              <Users className="h-3 w-3" />
              {t("activityDetails.ageRange")} {ageRange}
            </span>
          )}
          {activity.format && (
            <span className="flex items-center gap-1 border border-border/70 rounded-full px-2.5 py-1 bg-background/60">
              <Monitor className="h-3 w-3" />
              {translateEnum("format", activity.format)}
            </span>
          )}
          {activity.source && (
            <span className="flex items-center gap-1 border border-border/70 rounded-full px-2.5 py-1 bg-background/60">
              {t("activityDetails.source")}: {activity.source}
            </span>
          )}
        </div>
      </div>

      <DeleteActivityDialog
        open={deleteDialogOpen}
        activityName={activity.name}
        isLoading={deleteApi.isLoading}
        error={deleteApi.error}
        onOpenChange={setDeleteDialogOpen}
        onConfirm={handleDeleteActivity}
      />

      {/* Body: main content + sidebar */}
      <div className="grid grid-cols-1 lg:grid-cols-[1fr_256px] gap-6">
        {/* Main column */}
        <div className="space-y-6 min-w-0">
          {/* Description */}
          {activity.description && (
            <section className="space-y-2">
              <h2 className="text-base font-semibold text-foreground">
                {t("activityDetails.descriptionLabel")}
              </h2>
              <p className="text-sm text-muted-foreground leading-relaxed whitespace-pre-wrap">
                {activity.description}
              </p>
            </section>
          )}

          {/* Learning Level */}
          {activity.bloomLevel && (
            <section className="space-y-3">
              <h2 className="text-base font-semibold text-foreground">
                {t("activityDetails.bloomLevel")}
              </h2>
              <div className="flex flex-wrap gap-2">
                {BLOOM_ORDER.map((level) => {
                  const isActive = level === bloomKey;
                  const isPast =
                    bloomIndex >= 0 && BLOOM_ORDER.indexOf(level) <= bloomIndex;
                  return (
                    <span
                      key={level}
                      className={`px-3 py-1 rounded-full text-xs font-medium border transition-colors ${
                        isActive
                          ? "bg-primary text-primary-foreground border-primary"
                          : isPast
                            ? "bg-primary/10 text-primary border-primary/20"
                            : "bg-muted text-muted-foreground border-border"
                      }`}
                    >
                      {translateEnum("bloomLevel", level)}
                    </span>
                  );
                })}
              </div>
            </section>
          )}

          {/* Downloads */}
          {hasDownloads && (
            <section className="space-y-3">
              <h2 className="text-base font-semibold text-foreground">
                {t("activityDetails.downloads")}
              </h2>
              <div className="border border-border rounded-lg overflow-hidden divide-y divide-border">
                {activity.documents?.map((doc) => (
                  <div
                    key={doc.id}
                    className="flex items-center gap-3 px-4 py-3"
                  >
                    <FileText className="h-4 w-4 text-muted-foreground shrink-0" />
                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-medium truncate">
                        {doc.filename}
                      </p>
                      {Number.isFinite(doc.fileSize) && (
                        <p className="text-xs text-muted-foreground">
                          {(doc.fileSize / 1024).toFixed(1)} KB
                        </p>
                      )}
                    </div>
                    <button
                      type="button"
                      disabled={documentApi.isLoading}
                      onClick={() => handleOpenDocument(doc.id, doc.filename)}
                      className="shrink-0 inline-flex items-center gap-1 px-2.5 h-7 rounded text-xs font-medium bg-red-50 text-red-700 border border-red-200 hover:bg-red-100 transition-colors disabled:opacity-50"
                    >
                      PDF
                    </button>
                  </div>
                ))}

                {activity.markdowns
                  ?.slice()
                  .sort((a, b) => {
                    const order = [
                      "deckblatt",
                      "artikulationsschema",
                      "uebung",
                      "uebung_loesung",
                      "hintergrundwissen",
                      "tafelbild",
                    ];
                    const ai = order.indexOf(a.type ?? "");
                    const bi = order.indexOf(b.type ?? "");
                    return (
                      (ai === -1 ? order.length : ai) -
                      (bi === -1 ? order.length : bi)
                    );
                  })
                  .map((md) => (
                    <div
                      key={md.id}
                      className="flex items-center gap-3 px-4 py-3"
                    >
                      <BookOpen className="h-4 w-4 text-muted-foreground shrink-0" />
                      <div className="flex-1 min-w-0">
                        <p className="text-sm font-medium truncate">
                          {activity.name}
                          {md.type
                            ? ` — ${md.type.charAt(0).toUpperCase() + md.type.slice(1)}`
                            : ""}
                        </p>
                        {docxLoadingId === md.id && (
                          <p className="text-xs text-muted-foreground italic animate-pulse mt-0.5">
                            {docxWaitingQuotes[quoteIndex]}
                          </p>
                        )}
                      </div>
                      <div className="flex items-center gap-1.5 shrink-0">
                        <button
                          type="button"
                          onClick={() => handleDownloadMarkdownPdf(md.id)}
                          className="inline-flex items-center gap-1 px-2.5 h-7 rounded text-xs font-medium bg-red-50 text-red-700 border border-red-200 hover:bg-red-100 transition-colors"
                        >
                          PDF
                        </button>
                        {docxAvailable && (
                          <button
                            type="button"
                            disabled={!!docxLoadingId}
                            onClick={() =>
                              handleDownloadMarkdownDocx(md.id, md.type)
                            }
                            className="inline-flex items-center gap-1 px-2.5 h-7 rounded text-xs font-medium bg-blue-50 text-blue-700 border border-blue-200 hover:bg-blue-100 transition-colors disabled:opacity-50"
                          >
                            {docxLoadingId === md.id ? (
                              <Loader2 className="h-3 w-3 animate-spin" />
                            ) : (
                              "DOCX"
                            )}
                          </button>
                        )}
                      </div>
                    </div>
                  ))}

                {activity.markdowns && activity.markdowns.length > 0 && (
                  <div className="flex items-center gap-3 px-4 py-3 bg-muted/20">
                    <Download className="h-4 w-4 text-muted-foreground shrink-0" />
                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-medium">
                        {t("activityDetails.downloadActivity")}
                      </p>
                      {docxLoadingId === "activity" ? (
                        <p className="text-xs text-muted-foreground italic animate-pulse">
                          {docxWaitingQuotes[quoteIndex]}
                        </p>
                      ) : (
                        <p className="text-xs text-muted-foreground">
                          {t("activityDetails.combinedDocument")}
                        </p>
                      )}
                    </div>
                    <div className="flex items-center gap-1.5 shrink-0">
                      <button
                        type="button"
                        onClick={handleOpenActivityPdf}
                        className="inline-flex items-center gap-1 px-2.5 h-7 rounded text-xs font-medium bg-red-50 text-red-700 border border-red-200 hover:bg-red-100 transition-colors"
                      >
                        PDF
                      </button>
                      {docxAvailable && (
                        <button
                          type="button"
                          disabled={!!docxLoadingId}
                          onClick={handleOpenActivityDocx}
                          className="inline-flex items-center gap-1 px-2.5 h-7 rounded text-xs font-medium bg-blue-50 text-blue-700 border border-blue-200 hover:bg-blue-100 transition-colors disabled:opacity-50"
                        >
                          {docxLoadingId === "activity" ? (
                            <Loader2 className="h-3 w-3 animate-spin" />
                          ) : (
                            "DOCX"
                          )}
                        </button>
                      )}
                    </div>
                  </div>
                )}
              </div>
            </section>
          )}
        </div>

        {/* Sidebar */}
        <aside className="space-y-2.5">
          {activity.topics && activity.topics.length > 0 && (
            <div className="border border-border rounded-lg px-4 py-3">
              <p className="text-[10px] font-semibold text-muted-foreground uppercase tracking-wider mb-1.5">
                {t("activityDetails.topics")}
              </p>
              <div className="flex flex-wrap gap-1">
                {activity.topics.map((topic) => (
                  <Badge
                    key={topic}
                    variant="secondary"
                    className="text-xs font-medium px-2 py-0.5"
                  >
                    {translateEnum("topics", topic)}
                  </Badge>
                ))}
              </div>
            </div>
          )}
          {activity.mentalLoad && (
            <div className="border border-border rounded-lg px-4 py-3">
              <p className="text-[10px] font-semibold text-muted-foreground uppercase tracking-wider mb-1">
                {t("activityDetails.energyRequirements")} —{" "}
                {t("activityDetails.mental")}
              </p>
              <p className="text-sm font-medium text-foreground flex items-center gap-1.5">
                <Brain className="h-3.5 w-3.5 text-blue-500" />
                {translateEnum("energy", activity.mentalLoad)}
              </p>
            </div>
          )}
          {activity.physicalEnergy && (
            <div className="border border-border rounded-lg px-4 py-3">
              <p className="text-[10px] font-semibold text-muted-foreground uppercase tracking-wider mb-1">
                {t("activityDetails.energyRequirements")} —{" "}
                {t("activityDetails.physical")}
              </p>
              <p className="text-sm font-medium text-foreground flex items-center gap-1.5">
                <ActivityIcon className="h-3.5 w-3.5 text-orange-500" />
                {translateEnum("energy", activity.physicalEnergy)}
              </p>
            </div>
          )}
          {activity.resourcesNeeded && activity.resourcesNeeded.length > 0 && (
            <div className="border border-border rounded-lg px-4 py-3">
              <p className="text-[10px] font-semibold text-muted-foreground uppercase tracking-wider mb-1.5">
                {t("activityDetails.resourcesNeeded")}
              </p>
              <ul className="space-y-1">
                {activity.resourcesNeeded.map((resource, i) => (
                  <li
                    key={i}
                    className="flex items-center gap-1.5 text-sm font-medium text-foreground"
                  >
                    <span className="w-1.5 h-1.5 rounded-full bg-primary shrink-0" />
                    {translateEnum("resources", resource)}
                  </li>
                ))}
              </ul>
            </div>
          )}
        </aside>
      </div>
    </div>
  );
};
