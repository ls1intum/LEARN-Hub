import React from "react";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { ErrorDisplay } from "@/components/ui/ErrorDisplay";
import { AlertTriangle, Trash2 } from "lucide-react";
import { useTranslation } from "react-i18next";

interface DeleteActivityDialogProps {
  open: boolean;
  activityName?: string;
  isLoading?: boolean;
  error?: string | null;
  onOpenChange: (open: boolean) => void;
  onConfirm: () => void;
}

export const DeleteActivityDialog: React.FC<DeleteActivityDialogProps> = ({
  open,
  activityName,
  isLoading = false,
  error,
  onOpenChange,
  onConfirm,
}) => {
  const { t } = useTranslation();

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <AlertTriangle className="h-5 w-5 text-destructive" />
            {t("activityDetails.deleteTitle")}
          </DialogTitle>
          <DialogDescription>
            {t("activityDetails.deleteDescription", {
              name: activityName || t("activityDetails.activityFallbackName"),
            })}
          </DialogDescription>
        </DialogHeader>
        <ErrorDisplay error={error ?? null} />
        <DialogFooter>
          <Button
            type="button"
            variant="outline"
            onClick={() => onOpenChange(false)}
            disabled={isLoading}
          >
            {t("common.cancel")}
          </Button>
          <Button
            type="button"
            variant="destructive"
            onClick={onConfirm}
            disabled={isLoading}
            className="gap-1.5"
          >
            <Trash2 className="h-4 w-4" />
            {isLoading
              ? t("activityDetails.deleting")
              : t("activityDetails.confirmDelete")}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
};
