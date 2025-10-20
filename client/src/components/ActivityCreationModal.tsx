import React, { useState } from "react";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { ActivityForm } from "@/components/forms/ActivityForm";
import { apiService } from "@/services/apiService";
import type { Activity } from "@/types/activity";
import { logger } from "@/services/logger";

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
  document_id: number | null;
  [key: string]: unknown;
}

interface ActivityCreationModalProps {
  isOpen: boolean;
  onClose: () => void;
  extractedFields?: Record<string, unknown>;
  onSuccess?: (activity: Activity) => void;
  pdfDocumentId?: number;
}

export const ActivityCreationModal: React.FC<ActivityCreationModalProps> = ({
  isOpen,
  onClose,
  extractedFields,
  onSuccess,
  pdfDocumentId,
}) => {
  const [isCreating, setIsCreating] = useState(false);

  const handleSubmit = async (formData: ActivityFormData) => {
    setIsCreating(true);

    try {
      const response = (await apiService.createActivity({
        ...formData,
        document_id: formData.document_id || undefined,
      })) as {
        activity: Activity;
      };
      onSuccess?.(response.activity);
      onClose();
    } catch (error) {
      logger.error("Error creating activity", error, "ActivityCreationModal");
      // Re-throw the error so the form can handle it
      throw error;
    } finally {
      setIsCreating(false);
    }
  };

  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>Create Activity from PDF</DialogTitle>
          <DialogDescription>
            Create a new activity using the uploaded PDF document. Fields have
            been pre-filled with extracted data where available.
          </DialogDescription>
        </DialogHeader>

        <ActivityForm
          initialData={{
            ...extractedFields,
            document_id: pdfDocumentId || null,
          }}
          onSubmit={handleSubmit}
          onCancel={onClose}
          isLoading={isCreating}
        />
      </DialogContent>
    </Dialog>
  );
};
