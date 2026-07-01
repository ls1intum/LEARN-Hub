import React, { useCallback, useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { Breadcrumb } from "@/components/ui/Breadcrumb";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { DeleteActivityDialog } from "@/components/activities/DeleteActivityDialog";
import {
  AlertCircle,
  CheckCircle2,
  Clock,
  Edit3,
  FileUp,
  Loader2,
  Plus,
  Send,
  Trash2,
  TriangleAlert,
  Upload,
} from "lucide-react";
import { PageHeader } from "@/components/ui/PageHeader";
import { apiService } from "@/services/apiService";
import { useTranslateEnum } from "@/hooks/useTranslateEnum";
import type { Activity } from "@/types/activity";
import type { ActivityNavigationState } from "@/utils/activityNavigation";

const MAX_FILE_SIZE_BYTES = 1024 * 1024; // 1 MB

const MARKDOWN_KEYS = [
  "cover_sheet",
  "lesson_plan",
  "background_knowledge",
  "board_image",
  "exercise",
  "exercise_solution",
] as const;
type MarkdownKey = (typeof MARKDOWN_KEYS)[number];
const ALL_MARKDOWN_KEYS = [...MARKDOWN_KEYS];

// ─── Upload Modal ──────────────────────────────────────────────────────────

interface UploadModalProps {
  open: boolean;
  onClose: () => void;
  onCreated: (activity: Activity) => void;
}

const UploadModal: React.FC<UploadModalProps> = ({
  open,
  onClose,
  onCreated,
}) => {
  const { t } = useTranslation();
  const [file, setFile] = useState<File | null>(null);
  const [fileError, setFileError] = useState<string | null>(null);
  const [uploading, setUploading] = useState(false);
  const [uploadError, setUploadError] = useState<string | null>(null);
  const [dragOver, setDragOver] = useState(false);
  const [generateMetadata, setGenerateMetadata] = useState(true);
  const [markdownTypes, setMarkdownTypes] = useState<Set<MarkdownKey>>(
    new Set(ALL_MARKDOWN_KEYS),
  );
  const inputRef = useRef<HTMLInputElement>(null);

  const reset = () => {
    setFile(null);
    setFileError(null);
    setUploadError(null);
    setUploading(false);
    setGenerateMetadata(true);
    setMarkdownTypes(new Set(ALL_MARKDOWN_KEYS));
  };

  const toggleMarkdownType = (key: MarkdownKey, checked: boolean) => {
    setMarkdownTypes((prev) => {
      const next = new Set(prev);
      if (checked) {
        next.add(key);
        if (key === "board_image") next.add("lesson_plan");
      } else {
        next.delete(key);
        if (key === "lesson_plan") next.delete("board_image");
      }
      return next;
    });
  };

  const handleClose = () => {
    if (uploading) return;
    reset();
    onClose();
  };

  const validateFile = (f: File): string | null => {
    if (!f.name.toLowerCase().endsWith(".pdf"))
      return t("drafts.uploadModal.onlyPdf");
    if (f.size > MAX_FILE_SIZE_BYTES)
      return t("drafts.uploadModal.fileTooLarge", {
        size: (f.size / 1024).toFixed(0),
      });
    return null;
  };

  const handleFileSelect = (f: File) => {
    const err = validateFile(f);
    setFileError(err);
    setUploadError(null);
    setFile(err ? null : f);
  };

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const f = e.target.files?.[0];
    if (f) handleFileSelect(f);
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setDragOver(false);
    const f = e.dataTransfer.files?.[0];
    if (f) handleFileSelect(f);
  };

  const handleSubmit = async () => {
    if (!file) return;
    setUploading(true);
    setUploadError(null);
    try {
      const activity = await apiService.uploadAndCreatePending(file, {
        generateMetadata,
        markdownTypes: Array.from(markdownTypes),
      });
      reset();
      onCreated(activity);
      onClose();
    } catch (err: unknown) {
      const msg =
        err instanceof Error ? err.message : t("drafts.uploadModal.uploadError");
      setUploadError(msg);
      setUploading(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={(o) => !o && handleClose()}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>{t("drafts.uploadModal.title")}</DialogTitle>
        </DialogHeader>

        <div className="space-y-4 pt-2">
          {/* Drop zone */}
          <div
            className={`border-2 border-dashed rounded-lg p-8 flex flex-col items-center gap-3 cursor-pointer transition-colors ${
              dragOver
                ? "border-primary bg-primary/5"
                : "border-border hover:border-primary/50"
            }`}
            onClick={() => inputRef.current?.click()}
            onDragOver={(e) => {
              e.preventDefault();
              setDragOver(true);
            }}
            onDragLeave={() => setDragOver(false)}
            onDrop={handleDrop}
          >
            <FileUp className="h-10 w-10 text-muted-foreground" />
            {file ? (
              <div className="text-center">
                <p className="text-sm font-medium text-foreground">
                  {file.name}
                </p>
                <p className="text-xs text-muted-foreground mt-0.5">
                  {(file.size / 1024).toFixed(0)} KB
                </p>
              </div>
            ) : (
              <div className="text-center">
                <p className="text-sm text-muted-foreground">
                  {t("drafts.uploadModal.dropzoneText")}{" "}
                  <span className="text-primary font-medium">
                    {t("drafts.uploadModal.dropzoneSelect")}
                  </span>
                </p>
                <p className="text-xs text-muted-foreground mt-1">
                  {t("drafts.uploadModal.maxSize")}
                </p>
              </div>
            )}
          </div>

          <input
            ref={inputRef}
            type="file"
            accept=".pdf"
            className="hidden"
            onChange={handleInputChange}
          />

          {/* File error */}
          {fileError && (
            <div className="flex items-start gap-2 text-destructive text-sm">
              <AlertCircle className="h-4 w-4 mt-0.5 flex-shrink-0" />
              <span>{fileError}</span>
            </div>
          )}

          {/* Upload error */}
          {uploadError && (
            <div className="flex items-start gap-2 text-destructive text-sm bg-destructive/5 border border-destructive/20 rounded-md p-3">
              <AlertCircle className="h-4 w-4 mt-0.5 flex-shrink-0" />
              <span>{uploadError}</span>
            </div>
          )}

          {/* LLM generation options */}
          <div className="rounded-md border border-border p-3 space-y-3">
            <p className="text-xs font-medium text-muted-foreground uppercase tracking-wide">
              {t("drafts.uploadModal.aiGeneration")}
            </p>
            <label className="flex items-center gap-2 cursor-pointer select-none">
              <Checkbox
                checked={generateMetadata}
                onCheckedChange={(v) => setGenerateMetadata(v === true)}
                disabled={uploading}
              />
              <span className="text-sm">
                {t("drafts.uploadModal.extractMetadata")}
              </span>
            </label>
            <div className="space-y-1.5 pl-1">
              <p className="text-xs text-muted-foreground font-medium">
                {t("drafts.uploadModal.markdownsLabel")}
              </p>
              {MARKDOWN_KEYS.map((key) => (
                <label
                  key={key}
                  className="flex items-center gap-2 cursor-pointer select-none"
                >
                  <Checkbox
                    checked={markdownTypes.has(key)}
                    onCheckedChange={(v) => toggleMarkdownType(key, v === true)}
                    disabled={uploading}
                  />
                  <span className="text-sm">{t(`markdownTypes.${key}`)}</span>
                </label>
              ))}
            </div>
          </div>

          <div className="flex gap-2 justify-end">
            <Button
              variant="outline"
              onClick={handleClose}
              disabled={uploading}
            >
              {t("common.cancel")}
            </Button>
            <Button onClick={handleSubmit} disabled={!file || uploading}>
              {uploading ? (
                <>
                  <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                  {t("drafts.uploadModal.uploading")}
                </>
              ) : (
                <>
                  <Upload className="h-4 w-4 mr-2" />
                  {t("drafts.uploadModal.upload")}
                </>
              )}
            </Button>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
};

// ─── Pending Card ──────────────────────────────────────────────────────────

interface PendingCardProps {
  activity: Activity;
  onDelete: (id: string) => void;
}

const PendingCard: React.FC<PendingCardProps> = ({ activity, onDelete }) => {
  const { t } = useTranslation();
  const hasError = !!activity.generationError;

  return (
    <div
      className={`border rounded-lg p-4 flex items-start gap-4 ${
        hasError
          ? "border-destructive/40 bg-destructive/5"
          : "border-border bg-card"
      }`}
    >
      <div className="flex-shrink-0 mt-0.5">
        {hasError ? (
          <TriangleAlert className="h-5 w-5 text-destructive" />
        ) : (
          <Loader2 className="h-5 w-5 text-primary animate-spin" />
        )}
      </div>
      <div className="flex-1 min-w-0">
        <p className="text-sm font-medium text-foreground truncate">
          {activity.name}
        </p>
        {hasError ? (
          <p className="text-xs text-destructive mt-1 leading-relaxed">
            {t("drafts.pendingCard.generationError", {
              error: activity.generationError,
            })}
          </p>
        ) : (
          <p className="text-xs text-muted-foreground mt-0.5">
            {t("drafts.pendingCard.generating")}
          </p>
        )}
      </div>
      <Button
        variant="ghost"
        size="icon"
        className="flex-shrink-0 h-8 w-8 text-muted-foreground hover:text-destructive"
        onClick={() => onDelete(activity.id)}
        title={t("common.delete")}
      >
        <Trash2 className="h-4 w-4" />
      </Button>
    </div>
  );
};

// ─── Draft Card ────────────────────────────────────────────────────────────

interface DraftCardProps {
  activity: Activity;
  onPublish: (id: string) => void;
  publishing: boolean;
  onDelete: (id: string) => void;
}

const DraftCard: React.FC<DraftCardProps> = ({
  activity,
  onPublish,
  publishing,
  onDelete,
}) => {
  const { t } = useTranslation();
  const translateEnum = useTranslateEnum();
  const navigate = useNavigate();
  const detailPath = `/drafts/${activity.id}`;
  const detailNavigationState: ActivityNavigationState = {
    backTo: "/drafts",
  };
  const editNavigationState: ActivityNavigationState = {
    backTo: "/drafts",
    detailPath,
  };

  return (
    <div className="border border-border rounded-lg p-4 bg-card flex flex-col sm:flex-row sm:items-center gap-3">
      <div className="flex-shrink-0 hidden sm:block">
        <CheckCircle2 className="h-8 w-8 text-green-500" />
      </div>
      <div
        className="flex-1 min-w-0 cursor-pointer"
        onClick={() => navigate(detailPath, { state: detailNavigationState })}
      >
        <p className="text-sm font-semibold text-foreground truncate hover:underline">
          {activity.name}
        </p>
        <div className="flex flex-wrap gap-x-3 gap-y-0.5 mt-1">
          {activity.format && (
            <span className="text-xs text-muted-foreground capitalize">
              {translateEnum("format", activity.format)}
            </span>
          )}
          {activity.durationMinMinutes != null && (
            <span className="text-xs text-muted-foreground flex items-center gap-1">
              <Clock className="h-3 w-3" />
              {activity.durationMinMinutes}
              {activity.durationMaxMinutes != null &&
                activity.durationMaxMinutes !== activity.durationMinMinutes &&
                `–${activity.durationMaxMinutes}`}{" "}
              {t("activityCard.minutes")}
            </span>
          )}
          {activity.ageMin != null && (
            <span className="text-xs text-muted-foreground">
              {activity.ageMin}–{activity.ageMax} {t("activityCard.years")}
            </span>
          )}
        </div>
        {activity.description &&
          activity.description !== "Wird generiert..." && (
            <p className="text-xs text-muted-foreground mt-1 line-clamp-2">
              {activity.description}
            </p>
          )}
      </div>
      <div className="flex items-center gap-2 flex-shrink-0">
        <Button
          variant="ghost"
          size="icon"
          className="h-8 w-8 text-muted-foreground hover:text-destructive"
          onClick={() => onDelete(activity.id)}
          title={t("common.delete")}
        >
          <Trash2 className="h-4 w-4" />
        </Button>
        <Button
          variant="outline"
          size="sm"
          className="gap-1.5"
          onClick={() =>
            navigate(`/drafts/${activity.id}/edit`, {
              state: editNavigationState,
            })
          }
        >
          <Edit3 className="h-3.5 w-3.5" />
          {t("common.edit")}
        </Button>
        <Button
          size="sm"
          className="gap-1.5"
          onClick={() => onPublish(activity.id)}
          disabled={publishing}
        >
          {publishing ? (
            <Loader2 className="h-3.5 w-3.5 animate-spin" />
          ) : (
            <Send className="h-3.5 w-3.5" />
          )}
          {t("drafts.publish")}
        </Button>
      </div>
    </div>
  );
};

// ─── Main Page ─────────────────────────────────────────────────────────────

export const DraftsPage: React.FC = () => {
  const { t } = useTranslation();
  const [activities, setActivities] = useState<Activity[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [uploadOpen, setUploadOpen] = useState(false);
  const [publishingId, setPublishingId] = useState<string | null>(null);
  const [activityToDelete, setActivityToDelete] = useState<Activity | null>(
    null,
  );
  const [deleteError, setDeleteError] = useState<string | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);

  const pending = activities.filter(
    (a) => a.status === "PENDING" && !a.generationError,
  );
  const errored = activities.filter(
    (a) => a.status === "PENDING" && !!a.generationError,
  );
  const drafts = activities.filter((a) => a.status === "DRAFT");

  const fetchDrafts = useCallback(async () => {
    try {
      const data = await apiService.getDraftActivities();
      setActivities(data);
      setError(null);
    } catch (err: unknown) {
      setError(
        err instanceof Error ? err.message : t("drafts.loadError"),
      );
    } finally {
      setLoading(false);
    }
  }, [t]);

  // Initial load
  useEffect(() => {
    fetchDrafts();
  }, [fetchDrafts]);

  // Stream real-time status updates from the server instead of polling
  useEffect(() => {
    const baseUrl = (import.meta.env.VITE_API_SERVER as string) || "";
    const es = new EventSource(`${baseUrl}/api/activities/drafts/events`, {
      withCredentials: true,
    });

    es.addEventListener("draft-update", (event) => {
      const update = JSON.parse(event.data) as {
        id: string;
        status: Activity["status"];
        generationError: string;
      };
      setActivities((prev) =>
        prev.map((a) =>
          a.id === update.id
            ? {
                ...a,
                status: update.status,
                generationError: update.generationError || undefined,
              }
            : a,
        ),
      );
    });

    es.onerror = () => {
      fetchDrafts();
    };

    return () => es.close();
  }, [fetchDrafts]);

  const handleCreated = (activity: Activity) => {
    setActivities((prev) => [activity, ...prev]);
  };

  const handlePublish = async (id: string) => {
    setPublishingId(id);
    try {
      await apiService.publishActivity(id);
      setActivities((prev) => prev.filter((a) => a.id !== id));
    } catch (err: unknown) {
      alert(err instanceof Error ? err.message : t("drafts.publishError"));
    } finally {
      setPublishingId(null);
    }
  };

  const handleDelete = async () => {
    if (!activityToDelete) return;

    setIsDeleting(true);
    setDeleteError(null);
    try {
      await apiService.deleteActivity(activityToDelete.id);
      setActivities((prev) => prev.filter((a) => a.id !== activityToDelete.id));
      setActivityToDelete(null);
    } catch (err: unknown) {
      setDeleteError(
        err instanceof Error ? err.message : t("drafts.deleteError"),
      );
    } finally {
      setIsDeleting(false);
    }
  };

  return (
    <div className="py-6 space-y-8">
      <div>
        <Breadcrumb items={[{ label: t("nav.drafts") }]} className="mb-3" />
        <PageHeader
          title={t("drafts.title")}
          description={t("drafts.description")}
        >
          <Button onClick={() => setUploadOpen(true)} className="gap-2">
            <Plus className="h-4 w-4" />
            {t("drafts.new")}
          </Button>
        </PageHeader>
      </div>

      <UploadModal
        open={uploadOpen}
        onClose={() => setUploadOpen(false)}
        onCreated={handleCreated}
      />
      <DeleteActivityDialog
        open={!!activityToDelete}
        activityName={activityToDelete?.name}
        isLoading={isDeleting}
        error={deleteError}
        onOpenChange={(open) => {
          if (open) return;
          setActivityToDelete(null);
          setDeleteError(null);
        }}
        onConfirm={handleDelete}
      />

      {loading && (
        <div className="flex items-center justify-center py-16 text-muted-foreground">
          <Loader2 className="h-6 w-6 animate-spin mr-2" />
          {t("common.loading")}
        </div>
      )}

      {error && (
        <div className="flex items-start gap-2 text-destructive text-sm bg-destructive/5 border border-destructive/20 rounded-md p-4">
          <AlertCircle className="h-4 w-4 mt-0.5 flex-shrink-0" />
          {error}
        </div>
      )}

      {!loading && !error && (
        <>
          {/* Pending section */}
          {pending.length > 0 && (
            <section className="space-y-3">
              <div className="flex items-center gap-2">
                <Clock className="h-4 w-4 text-muted-foreground" />
                <h2 className="text-sm font-semibold text-muted-foreground uppercase tracking-wide">
                  {t("drafts.inProgress")} ({pending.length})
                </h2>
              </div>
              <div className="space-y-2">
                {pending.map((a) => (
                  <PendingCard
                    key={a.id}
                    activity={a}
                    onDelete={(id) =>
                      setActivityToDelete(
                        activities.find((activity) => activity.id === id) ||
                          null,
                      )
                    }
                  />
                ))}
              </div>
            </section>
          )}

          {/* Errored section */}
          {errored.length > 0 && (
            <section className="space-y-3">
              <div className="flex items-center gap-2">
                <TriangleAlert className="h-4 w-4 text-destructive" />
                <h2 className="text-sm font-semibold text-destructive uppercase tracking-wide">
                  {t("drafts.failed")} ({errored.length})
                </h2>
              </div>
              <div className="space-y-2">
                {errored.map((a) => (
                  <PendingCard
                    key={a.id}
                    activity={a}
                    onDelete={(id) =>
                      setActivityToDelete(
                        activities.find((activity) => activity.id === id) ||
                          null,
                      )
                    }
                  />
                ))}
              </div>
            </section>
          )}

          {/* Drafts section */}
          {drafts.length > 0 && (
            <section className="space-y-3">
              <div className="flex items-center gap-2">
                <CheckCircle2 className="h-4 w-4 text-green-500" />
                <h2 className="text-sm font-semibold text-muted-foreground uppercase tracking-wide">
                  {t("drafts.title")} ({drafts.length})
                </h2>
              </div>
              <div className="space-y-2">
                {drafts.map((a) => (
                  <DraftCard
                    key={a.id}
                    activity={a}
                    onPublish={handlePublish}
                    publishing={publishingId === a.id}
                    onDelete={(id) =>
                      setActivityToDelete(
                        activities.find((activity) => activity.id === id) ||
                          null,
                      )
                    }
                  />
                ))}
              </div>
            </section>
          )}

          {/* Empty state */}
          {pending.length === 0 &&
            errored.length === 0 &&
            drafts.length === 0 && (
              <div className="text-center py-20 space-y-4">
                <FileUp className="h-12 w-12 text-muted-foreground mx-auto" />
                <p className="text-muted-foreground text-sm">
                  {t("drafts.emptyStatePrefix")}
                  <strong>{t("drafts.new")}</strong>
                  {t("drafts.emptyStateSuffix")}
                </p>
              </div>
            )}
        </>
      )}
    </div>
  );
};
