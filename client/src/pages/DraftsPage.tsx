import React, { useCallback, useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";
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
import type { Activity } from "@/types/activity";
import type { ActivityNavigationState } from "@/utils/activityNavigation";

const MAX_FILE_SIZE_BYTES = 1024 * 1024; // 1 MB
const POLL_INTERVAL_MS = 5000;

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
  const [file, setFile] = useState<File | null>(null);
  const [fileError, setFileError] = useState<string | null>(null);
  const [uploading, setUploading] = useState(false);
  const [uploadError, setUploadError] = useState<string | null>(null);
  const [dragOver, setDragOver] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);

  const reset = () => {
    setFile(null);
    setFileError(null);
    setUploadError(null);
    setUploading(false);
  };

  const handleClose = () => {
    if (uploading) return;
    reset();
    onClose();
  };

  const validateFile = (f: File): string | null => {
    if (!f.name.toLowerCase().endsWith(".pdf"))
      return "Nur PDF-Dateien werden akzeptiert.";
    if (f.size > MAX_FILE_SIZE_BYTES)
      return `Die Datei ist zu groß (${(f.size / 1024).toFixed(0)} KB). Maximal erlaubt: 1 MB.`;
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
      const activity = await apiService.uploadAndCreatePending(file);
      reset();
      onCreated(activity);
      onClose();
    } catch (err: unknown) {
      const msg =
        err instanceof Error ? err.message : "Fehler beim Hochladen.";
      setUploadError(msg);
      setUploading(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={(o) => !o && handleClose()}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>PDF hochladen</DialogTitle>
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
                  PDF hier ablegen oder{" "}
                  <span className="text-primary font-medium">auswählen</span>
                </p>
                <p className="text-xs text-muted-foreground mt-1">
                  Maximal 1 MB
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

          <div className="flex gap-2 justify-end">
            <Button variant="outline" onClick={handleClose} disabled={uploading}>
              Abbrechen
            </Button>
            <Button onClick={handleSubmit} disabled={!file || uploading}>
              {uploading ? (
                <>
                  <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                  Hochladen…
                </>
              ) : (
                <>
                  <Upload className="h-4 w-4 mr-2" />
                  Hochladen
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
            Fehler bei der Generierung: {activity.generationError}
          </p>
        ) : (
          <p className="text-xs text-muted-foreground mt-0.5">
            Wird generiert…
          </p>
        )}
      </div>
      <Button
        variant="ghost"
        size="icon"
        className="flex-shrink-0 h-8 w-8 text-muted-foreground hover:text-destructive"
        onClick={() => onDelete(activity.id)}
        title="Löschen"
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
  const navigate = useNavigate();
  const detailNavigationState: ActivityNavigationState = {
    backTo: "/drafts",
  };

  return (
    <div className="border border-border rounded-lg p-4 bg-card flex flex-col sm:flex-row sm:items-center gap-3">
      <div className="flex-shrink-0 hidden sm:block">
        <CheckCircle2 className="h-8 w-8 text-green-500" />
      </div>
      <div className="flex-1 min-w-0">
        <p className="text-sm font-semibold text-foreground truncate">
          {activity.name}
        </p>
        <div className="flex flex-wrap gap-x-3 gap-y-0.5 mt-1">
          {activity.format && (
            <span className="text-xs text-muted-foreground capitalize">
              {activity.format}
            </span>
          )}
          {activity.durationMinMinutes != null && (
            <span className="text-xs text-muted-foreground flex items-center gap-1">
              <Clock className="h-3 w-3" />
              {activity.durationMinMinutes}
              {activity.durationMaxMinutes != null &&
                activity.durationMaxMinutes !== activity.durationMinMinutes &&
                `–${activity.durationMaxMinutes}`}{" "}
              Min.
            </span>
          )}
          {activity.ageMin != null && (
            <span className="text-xs text-muted-foreground">
              {activity.ageMin}–{activity.ageMax} Jahre
            </span>
          )}
        </div>
        {activity.description && activity.description !== "Wird generiert..." && (
          <p className="text-xs text-muted-foreground mt-1 line-clamp-2">
            {activity.description}
          </p>
        )}
      </div>
      <div className="flex items-center gap-2 flex-shrink-0">
        <Button
          variant="outline"
          size="sm"
          className="gap-1.5"
          onClick={() =>
            navigate(`/activity-edit/${activity.id}`, {
              state: detailNavigationState,
            })
          }
        >
          <Edit3 className="h-3.5 w-3.5" />
          Bearbeiten
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
          Veröffentlichen
        </Button>
        <Button
          variant="ghost"
          size="icon"
          className="h-8 w-8 text-muted-foreground hover:text-destructive"
          onClick={() => onDelete(activity.id)}
          title="Löschen"
        >
          <Trash2 className="h-4 w-4" />
        </Button>
      </div>
    </div>
  );
};

// ─── Main Page ─────────────────────────────────────────────────────────────

export const DraftsPage: React.FC = () => {
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
  const pollTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

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
        err instanceof Error ? err.message : "Fehler beim Laden der Entwürfe.",
      );
    } finally {
      setLoading(false);
    }
  }, []);

  // Initial load
  useEffect(() => {
    fetchDrafts();
  }, [fetchDrafts]);

  // Poll while there are actively-generating PENDING activities (not errored)
  useEffect(() => {
    if (pollTimerRef.current) clearTimeout(pollTimerRef.current);
    const hasPending = activities.some(
      (a) => a.status === "PENDING" && !a.generationError,
    );
    if (hasPending) {
      pollTimerRef.current = setTimeout(() => {
        fetchDrafts();
      }, POLL_INTERVAL_MS);
    }
    return () => {
      if (pollTimerRef.current) clearTimeout(pollTimerRef.current);
    };
  }, [activities, fetchDrafts]);

  const handleCreated = (activity: Activity) => {
    setActivities((prev) => [activity, ...prev]);
  };

  const handlePublish = async (id: string) => {
    setPublishingId(id);
    try {
      await apiService.publishActivity(id);
      setActivities((prev) => prev.filter((a) => a.id !== id));
    } catch (err: unknown) {
      alert(
        err instanceof Error
          ? err.message
          : "Fehler beim Veröffentlichen.",
      );
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
        err instanceof Error ? err.message : "Fehler beim Löschen.",
      );
    } finally {
      setIsDeleting(false);
    }
  };

  return (
    <div className="p-4 sm:p-6 max-w-3xl mx-auto space-y-8">
      <PageHeader title="Entwürfe" description="PDF hochladen, generieren und veröffentlichen">
        <Button onClick={() => setUploadOpen(true)} className="gap-2">
          <Plus className="h-4 w-4" />
          Neu
        </Button>
      </PageHeader>

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
          Laden…
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
                  In Bearbeitung ({pending.length})
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
                  Fehlgeschlagen ({errored.length})
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
                  Entwürfe ({drafts.length})
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
          {pending.length === 0 && errored.length === 0 && drafts.length === 0 && (
            <div className="text-center py-20 space-y-4">
              <FileUp className="h-12 w-12 text-muted-foreground mx-auto" />
              <p className="text-muted-foreground text-sm">
                Noch keine Entwürfe. Klicke auf{" "}
                <strong>Neu</strong>, um eine PDF hochzuladen.
              </p>
            </div>
          )}
        </>
      )}
    </div>
  );
};
