import React, { useEffect, useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { CheckCircle, AlertCircle } from "lucide-react";
import { FormField } from "@/components/ui/FormField";
import { NumberField } from "@/components/ui/NumberField";
import { SelectField } from "@/components/ui/SelectField";
import { BadgeSelector } from "@/components/ui/BadgeSelector";
import { useFieldValues } from "@/hooks/useFieldValues";
import type { FormFieldData } from "@/types/api";
import { useTranslation } from "react-i18next";

export interface ActivityFormData extends FormFieldData {
  name: string;
  description: string;
  source: string;
  ageMin: number;
  ageMax: number;
  format: string;
  bloomLevel: string;
  durationMinMinutes: number;
  durationMaxMinutes: number;
  mentalLoad: string;
  physicalEnergy: string;
  prepTimeMinutes: number;
  cleanupTimeMinutes: number;
  resourcesNeeded: string[];
  topics: string[];
  documentId: number | string | null;
}

interface ActivityFormProps {
  initialData?: Partial<ActivityFormData>;
  onSubmit: (data: ActivityFormData) => Promise<void>;
  onCancel: () => void;
  isLoading?: boolean;
  submitLabel?: string;
  cancelLabel?: string;
  submitIcon?: React.ReactNode;
  cancelIcon?: React.ReactNode;
  /** When true, the built-in footer buttons are hidden (navigation is handled externally) */
  hideButtons?: boolean;
  /** HTML id for the <form> element, allowing external submit buttons via form="" attribute */
  formId?: string;
}

const defaultFormData: ActivityFormData = {
  name: "",
  description: "",
  source: "",
  ageMin: 6,
  ageMax: 12,
  format: "",
  bloomLevel: "",
  durationMinMinutes: 15,
  durationMaxMinutes: 15,
  mentalLoad: "medium",
  physicalEnergy: "medium",
  prepTimeMinutes: 5,
  cleanupTimeMinutes: 5,
  resourcesNeeded: [],
  topics: [],
  documentId: null,
};

const normalizeFormData = (
  initialData?: Partial<ActivityFormData>,
): ActivityFormData => {
  const mergedData = {
    ...defaultFormData,
    ...initialData,
  };

  if (initialData?.durationMaxMinutes == null) {
    mergedData.durationMaxMinutes =
      initialData?.durationMinMinutes ?? defaultFormData.durationMinMinutes;
  }

  return mergedData;
};

export const ActivityForm: React.FC<ActivityFormProps> = ({
  initialData,
  onSubmit,
  onCancel,
  isLoading = false,
  submitLabel = "Create Activity",
  cancelLabel = "Cancel",
  submitIcon,
  cancelIcon,
  hideButtons = false,
  formId,
}) => {
  const { fieldValues } = useFieldValues();
  const { t } = useTranslation();
  const [formData, setFormData] = useState<ActivityFormData>(() =>
    normalizeFormData(initialData),
  );
  const [error, setError] = useState("");

  useEffect(() => {
    if (!initialData) return;

    setFormData(normalizeFormData(initialData));
  }, [initialData]);

  // Form field options - use translated labels
  const FORMAT_OPTIONS = [
    { value: "digital", label: t("enums.format.digital") },
    { value: "hybrid", label: t("enums.format.hybrid") },
    { value: "unplugged", label: t("enums.format.unplugged") },
  ];

  const BLOOM_LEVEL_OPTIONS = [
    { value: "remember", label: t("enums.bloomLevel.remember") },
    { value: "understand", label: t("enums.bloomLevel.understand") },
    { value: "apply", label: t("enums.bloomLevel.apply") },
    { value: "analyze", label: t("enums.bloomLevel.analyze") },
    { value: "evaluate", label: t("enums.bloomLevel.evaluate") },
    { value: "create", label: t("enums.bloomLevel.create") },
  ];

  const ENERGY_OPTIONS = [
    { value: "low", label: t("enums.energy.low") },
    { value: "medium", label: t("enums.energy.medium") },
    { value: "high", label: t("enums.energy.high") },
  ];

  const updateField = (
    field: keyof ActivityFormData,
    value: string | number | boolean | string[] | null | undefined,
  ) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
  };

  const toggleArrayValue = (field: keyof ActivityFormData, value: string) => {
    const currentArray = (formData[field] as string[]) || [];
    const newArray = currentArray.includes(value)
      ? currentArray.filter((v) => v !== value)
      : [...currentArray, value];
    updateField(field, newArray);
  };

  const validateForm = (): string | null => {
    if (!formData.name.trim()) return "Activity name is required";
    if (!formData.description.trim()) return "Activity description is required";
    if (formData.description.trim().length < 25)
      return "Description must be at least 25 characters long";
    if (formData.description.trim().length > 1000)
      return "Description must be 1000 characters or less";
    if (formData.ageMin < 6 || formData.ageMin > 15)
      return "Minimum age must be between 6 and 15";
    if (formData.ageMax < 6 || formData.ageMax > 15)
      return "Maximum age must be between 6 and 15";
    if (formData.ageMax < formData.ageMin)
      return "Maximum age must be greater than or equal to minimum age";
    if (!formData.format) return "Format is required";
    if (!formData.bloomLevel) return "Bloom's taxonomy level is required";
    if (!formData.documentId) return "PDF document is required";
    if (formData.durationMinMinutes < 1 || formData.durationMinMinutes > 300) {
      return "Duration must be between 1 and 300 minutes";
    }
    if (
      formData.durationMaxMinutes &&
      formData.durationMaxMinutes < formData.durationMinMinutes
    ) {
      return "Maximum duration must be greater than or equal to minimum duration";
    }
    // Validate prep time is in 5-minute increments
    if (formData.prepTimeMinutes % 5 !== 0) {
      return "Preparation time must be in 5-minute increments (0, 5, 10, 15, etc.)";
    }
    // Validate cleanup time is in 5-minute increments
    if (formData.cleanupTimeMinutes % 5 !== 0) {
      return "Cleanup time must be in 5-minute increments (0, 5, 10, 15, etc.)";
    }
    return null;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");

    const validationError = validateForm();
    if (validationError) {
      setError(validationError);
      return;
    }

    try {
      await onSubmit(formData);
    } catch (err) {
      setError(
        err instanceof Error ? err.message : "An unexpected error occurred",
      );
    }
  };

  return (
    <form id={formId} onSubmit={handleSubmit} className="space-y-6">
      {error && (
        <div className="p-3 bg-destructive/10 border border-destructive text-destructive rounded">
          {error}
        </div>
      )}

      {/* Basic Information */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <FormField
          label={t("activityForm.activityName")}
          required
          htmlFor="activity-name"
        >
          <Input
            id="activity-name"
            value={formData.name}
            onChange={(e) => updateField("name", e.target.value)}
            placeholder={t("activityForm.enterName")}
          />
        </FormField>
        <FormField
          label={t("activityForm.sourceLabel")}
          htmlFor="activity-source"
        >
          <Input
            id="activity-source"
            value={formData.source}
            onChange={(e) => updateField("source", e.target.value)}
            placeholder={t("activityForm.enterSource")}
          />
        </FormField>
      </div>

      <FormField
        label={t("activityForm.activityDescription")}
        required
        htmlFor="activity-description"
      >
        <textarea
          id="activity-description"
          value={formData.description}
          onChange={(e) => updateField("description", e.target.value)}
          placeholder={t("activityForm.enterDescription")}
          className="w-full min-h-[100px] px-3 py-2 border border-input bg-background rounded-md text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
          maxLength={1000}
        />
        <p className="text-xs text-muted-foreground mt-1">
          {t("activityForm.characters", { count: formData.description.length })}
        </p>
      </FormField>

      {/* Age Range */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <FormField
          label={t("activityForm.minAge")}
          required
          htmlFor="minimum-age"
        >
          <NumberField
            id="minimum-age"
            value={formData.ageMin}
            onChange={(value) => updateField("ageMin", value)}
            min={6}
            max={15}
          />
        </FormField>
        <FormField
          label={t("activityForm.maxAge")}
          required
          htmlFor="maximum-age"
        >
          <NumberField
            id="maximum-age"
            value={formData.ageMax}
            onChange={(value) => updateField("ageMax", value)}
            min={6}
            max={15}
          />
        </FormField>
      </div>

      {/* Format and Bloom Level */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <FormField label={t("activityForm.format")} required>
          <SelectField
            value={formData.format}
            onValueChange={(value) => updateField("format", value)}
            options={FORMAT_OPTIONS}
            placeholder={t("activityForm.selectFormat")}
          />
        </FormField>
        <FormField label={t("activityForm.bloomLevel")} required>
          <SelectField
            value={formData.bloomLevel}
            onValueChange={(value) => updateField("bloomLevel", value)}
            options={BLOOM_LEVEL_OPTIONS}
            placeholder={t("activityForm.selectBloom")}
          />
        </FormField>
      </div>

      {/* Duration */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <FormField
          label={t("activityForm.minDuration")}
          required
          htmlFor="minimum-duration"
        >
          <NumberField
            id="minimum-duration"
            value={formData.durationMinMinutes}
            onChange={(value) => updateField("durationMinMinutes", value)}
            min={1}
            max={300}
          />
        </FormField>
        <FormField
          label={t("activityForm.maxDuration")}
          htmlFor="maximum-duration"
        >
          <NumberField
            id="maximum-duration"
            value={formData.durationMaxMinutes}
            onChange={(value) => updateField("durationMaxMinutes", value)}
            min={1}
            max={300}
          />
        </FormField>
      </div>

      {/* Energy Levels */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <FormField label={t("activityForm.mentalLoad")}>
          <SelectField
            value={formData.mentalLoad}
            onValueChange={(value) => updateField("mentalLoad", value)}
            options={ENERGY_OPTIONS}
          />
        </FormField>
        <FormField label={t("activityForm.physicalEnergy")}>
          <SelectField
            value={formData.physicalEnergy}
            onValueChange={(value) => updateField("physicalEnergy", value)}
            options={ENERGY_OPTIONS}
          />
        </FormField>
      </div>

      {/* Prep and Cleanup Time */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <FormField
          label={t("activityForm.prepTime")}
          htmlFor="preparation-time"
        >
          <NumberField
            id="preparation-time"
            value={formData.prepTimeMinutes}
            onChange={(value) => updateField("prepTimeMinutes", value)}
            min={0}
            max={60}
            step={5}
          />
        </FormField>
        <FormField label={t("activityForm.cleanupTime")} htmlFor="cleanup-time">
          <NumberField
            id="cleanup-time"
            value={formData.cleanupTimeMinutes}
            onChange={(value) => updateField("cleanupTimeMinutes", value)}
            min={0}
            max={60}
            step={5}
          />
        </FormField>
      </div>

      {/* PDF Document Status */}
      <FormField label={t("activityForm.pdfDocument")}>
        {formData.documentId ? (
          <div className="mt-2 p-3 bg-success/10 border border-success/20 rounded-lg">
            <div className="flex items-center gap-2">
              <CheckCircle className="h-4 w-4 text-success" />
              <div>
                <p className="font-medium text-foreground">
                  {t("upload.pdfDocReady")}
                </p>
                <p className="text-sm text-muted-foreground">
                  Document ID: {formData.documentId}
                </p>
              </div>
            </div>
          </div>
        ) : (
          <div className="mt-2 p-3 bg-destructive/10 border border-destructive/20 rounded-lg">
            <div className="flex items-center gap-2">
              <AlertCircle className="h-4 w-4 text-destructive" />
              <p className="font-medium text-foreground">
                {t("upload.noPdfAttached")}
              </p>
            </div>
          </div>
        )}
      </FormField>

      {/* Resources and Topics */}
      <BadgeSelector
        label={t("activityForm.resourcesNeeded")}
        options={fieldValues?.resourcesAvailable || []}
        selectedValues={formData.resourcesNeeded}
        onToggle={(value) => toggleArrayValue("resourcesNeeded", value)}
        labelFn={(value) => {
          const key = `enums.resources.${value}`;
          const translated = t(key);
          return translated === key ? value : translated;
        }}
      />

      <BadgeSelector
        label={t("activityForm.topicsLabel")}
        options={fieldValues?.topics || []}
        selectedValues={formData.topics}
        onToggle={(value) => toggleArrayValue("topics", value)}
        labelFn={(value) => {
          const key = `enums.topics.${value}`;
          const translated = t(key);
          return translated === key ? value : translated;
        }}
      />

      {!hideButtons && (
        <div className="flex gap-2 justify-end pt-2">
          <Button type="button" variant="outline" onClick={onCancel}>
            {cancelIcon}
            {cancelLabel}
          </Button>
          <Button type="submit" disabled={isLoading}>
            {isLoading ? "Creating..." : submitLabel}
            {!isLoading && submitIcon}
          </Button>
        </div>
      )}
    </form>
  );
};
