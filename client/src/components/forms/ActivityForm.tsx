import React, { useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { CheckCircle, AlertCircle } from "lucide-react";
import { FormField } from "@/components/ui/FormField";
import { NumberField } from "@/components/ui/NumberField";
import { SelectField } from "@/components/ui/SelectField";
import { TagList } from "@/components/ui/TagList";
import type { FormFieldData } from "@/types/api";

interface ActivityFormData extends FormFieldData {
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
}

interface ActivityFormProps {
  initialData?: Partial<ActivityFormData>;
  onSubmit: (data: ActivityFormData) => Promise<void>;
  onCancel: () => void;
  isLoading?: boolean;
}

const defaultFormData: ActivityFormData = {
  name: "",
  description: "",
  source: "",
  age_min: 6,
  age_max: 12,
  format: "",
  bloom_level: "",
  duration_min_minutes: 15,
  duration_max_minutes: 30,
  mental_load: "medium",
  physical_energy: "medium",
  prep_time_minutes: 5,
  cleanup_time_minutes: 5,
  resources_needed: [],
  topics: [],
  document_id: null,
};

export const ActivityForm: React.FC<ActivityFormProps> = ({
  initialData,
  onSubmit,
  onCancel,
  isLoading = false,
}) => {
  const [formData, setFormData] = useState<ActivityFormData>({
    ...defaultFormData,
    ...initialData,
  });
  const [newResource, setNewResource] = useState("");
  const [newTopic, setNewTopic] = useState("");
  const [error, setError] = useState("");

  // Form field options
  const FORMAT_OPTIONS = [
    { value: "digital", label: "Digital" },
    { value: "hybrid", label: "Hybrid" },
    { value: "unplugged", label: "Unplugged" },
  ];

  const BLOOM_LEVEL_OPTIONS = [
    { value: "remember", label: "Remember" },
    { value: "understand", label: "Understand" },
    { value: "apply", label: "Apply" },
    { value: "analyze", label: "Analyze" },
    { value: "evaluate", label: "Evaluate" },
    { value: "create", label: "Create" },
  ];

  const ENERGY_OPTIONS = [
    { value: "low", label: "Low" },
    { value: "medium", label: "Medium" },
    { value: "high", label: "High" },
  ];

  const updateField = (
    field: keyof ActivityFormData,
    value: string | number | boolean | string[] | null | undefined,
  ) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
  };

  const addToList = (
    list: string[],
    newItem: string,
    setList: (items: string[]) => void,
  ) => {
    const trimmed = newItem.trim();
    if (trimmed && !list.includes(trimmed) && trimmed.length <= 50) {
      setList([...list, trimmed]);
      return true;
    }
    return false;
  };

  const addResource = () => {
    if (
      addToList(formData.resources_needed, newResource, (items) =>
        updateField("resources_needed", items),
      )
    ) {
      setNewResource("");
    } else if (newResource.trim().length > 50) {
      setError("Resource name must be 50 characters or less");
    }
  };

  const addTopic = () => {
    if (
      addToList(formData.topics, newTopic, (items) =>
        updateField("topics", items),
      )
    ) {
      setNewTopic("");
    } else if (newTopic.trim().length > 50) {
      setError("Topic name must be 50 characters or less");
    }
  };

  const removeResource = (index: number) => {
    const newResources = formData.resources_needed.filter(
      (_, i) => i !== index,
    );
    updateField("resources_needed", newResources);
  };

  const removeTopic = (index: number) => {
    const newTopics = formData.topics.filter((_, i) => i !== index);
    updateField("topics", newTopics);
  };

  const validateForm = (): string | null => {
    if (!formData.name.trim()) return "Activity name is required";
    if (!formData.description.trim()) return "Activity description is required";
    if (formData.description.trim().length < 25)
      return "Description must be at least 25 characters long";
    if (formData.description.trim().length > 1000)
      return "Description must be 1000 characters or less";
    if (formData.age_min < 6 || formData.age_min > 15)
      return "Minimum age must be between 6 and 15";
    if (formData.age_max < 6 || formData.age_max > 15)
      return "Maximum age must be between 6 and 15";
    if (formData.age_max < formData.age_min)
      return "Maximum age must be greater than or equal to minimum age";
    if (!formData.format) return "Format is required";
    if (!formData.bloom_level) return "Bloom's taxonomy level is required";
    if (!formData.document_id) return "PDF document is required";
    if (
      formData.duration_min_minutes < 1 ||
      formData.duration_min_minutes > 300
    ) {
      return "Duration must be between 1 and 300 minutes";
    }
    if (
      formData.duration_max_minutes &&
      formData.duration_max_minutes < formData.duration_min_minutes
    ) {
      return "Maximum duration must be greater than or equal to minimum duration";
    }
    // Validate prep time is in 5-minute increments
    if (formData.prep_time_minutes % 5 !== 0) {
      return "Preparation time must be in 5-minute increments (0, 5, 10, 15, etc.)";
    }
    // Validate cleanup time is in 5-minute increments
    if (formData.cleanup_time_minutes % 5 !== 0) {
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
    <form onSubmit={handleSubmit} className="space-y-6">
      {error && (
        <div className="p-3 bg-destructive/10 border border-destructive text-destructive rounded">
          {error}
        </div>
      )}

      {/* Basic Information */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <FormField label="Activity Name" required htmlFor="activity-name">
          <Input
            id="activity-name"
            value={formData.name}
            onChange={(e) => updateField("name", e.target.value)}
            placeholder="Enter activity name"
          />
        </FormField>
        <FormField label="Source" htmlFor="activity-source">
          <Input
            id="activity-source"
            value={formData.source}
            onChange={(e) => updateField("source", e.target.value)}
            placeholder="Enter source (e.g., curriculum, book)"
          />
        </FormField>
      </div>

      <FormField
        label="Activity Description"
        required
        htmlFor="activity-description"
      >
        <textarea
          id="activity-description"
          value={formData.description}
          onChange={(e) => updateField("description", e.target.value)}
          placeholder="Enter a detailed description of the activity (minimum 25 characters)"
          className="w-full min-h-[100px] px-3 py-2 border border-input bg-background rounded-md text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
          maxLength={1000}
        />
        <p className="text-xs text-muted-foreground mt-1">
          {formData.description.length}/1000 characters (minimum 25)
        </p>
      </FormField>

      {/* Age Range */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <FormField label="Minimum Age" required htmlFor="minimum-age">
          <NumberField
            id="minimum-age"
            value={formData.age_min}
            onChange={(value) => updateField("age_min", value)}
            min={6}
            max={15}
          />
        </FormField>
        <FormField label="Maximum Age" required htmlFor="maximum-age">
          <NumberField
            id="maximum-age"
            value={formData.age_max}
            onChange={(value) => updateField("age_max", value)}
            min={6}
            max={15}
          />
        </FormField>
      </div>

      {/* Format and Bloom Level */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <FormField label="Format" required>
          <SelectField
            value={formData.format}
            onValueChange={(value) => updateField("format", value)}
            options={FORMAT_OPTIONS}
            placeholder="Select format"
          />
        </FormField>
        <FormField label="Bloom's Taxonomy Level" required>
          <SelectField
            value={formData.bloom_level}
            onValueChange={(value) => updateField("bloom_level", value)}
            options={BLOOM_LEVEL_OPTIONS}
            placeholder="Select Bloom level"
          />
        </FormField>
      </div>

      {/* Duration */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <FormField
          label="Minimum Duration (minutes)"
          required
          htmlFor="minimum-duration"
        >
          <NumberField
            id="minimum-duration"
            value={formData.duration_min_minutes}
            onChange={(value) => updateField("duration_min_minutes", value)}
            min={1}
            max={300}
          />
        </FormField>
        <FormField
          label="Maximum Duration (minutes)"
          htmlFor="maximum-duration"
        >
          <NumberField
            id="maximum-duration"
            value={formData.duration_max_minutes}
            onChange={(value) => updateField("duration_max_minutes", value)}
            min={1}
            max={300}
          />
        </FormField>
      </div>

      {/* Energy Levels */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <FormField label="Mental Load">
          <SelectField
            value={formData.mental_load}
            onValueChange={(value) => updateField("mental_load", value)}
            options={ENERGY_OPTIONS}
          />
        </FormField>
        <FormField label="Physical Energy">
          <SelectField
            value={formData.physical_energy}
            onValueChange={(value) => updateField("physical_energy", value)}
            options={ENERGY_OPTIONS}
          />
        </FormField>
      </div>

      {/* Prep and Cleanup Time */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <FormField
          label="Preparation Time (minutes)"
          htmlFor="preparation-time"
        >
          <NumberField
            id="preparation-time"
            value={formData.prep_time_minutes}
            onChange={(value) => updateField("prep_time_minutes", value)}
            min={0}
            max={60}
            step={5}
          />
        </FormField>
        <FormField label="Cleanup Time (minutes)" htmlFor="cleanup-time">
          <NumberField
            id="cleanup-time"
            value={formData.cleanup_time_minutes}
            onChange={(value) => updateField("cleanup_time_minutes", value)}
            min={0}
            max={60}
            step={5}
          />
        </FormField>
      </div>

      {/* PDF Document Status */}
      <FormField label="PDF Document">
        {formData.document_id ? (
          <div className="mt-2 p-3 bg-success/10 border border-success/20 rounded-lg">
            <div className="flex items-center gap-2">
              <CheckCircle className="h-4 w-4 text-success" />
              <div>
                <p className="font-medium text-success-foreground">
                  PDF Document Ready
                </p>
                <p className="text-sm text-muted-foreground">
                  Document ID: {formData.document_id}
                </p>
              </div>
            </div>
          </div>
        ) : (
          <div className="mt-2 p-3 bg-destructive/10 border border-destructive/20 rounded-lg">
            <div className="flex items-center gap-2">
              <AlertCircle className="h-4 w-4 text-destructive" />
              <p className="font-medium text-destructive">
                No PDF document attached
              </p>
            </div>
          </div>
        )}
      </FormField>

      {/* Resources and Topics */}
      <FormField label="Resources Needed">
        <TagList
          items={formData.resources_needed}
          onRemove={removeResource}
          placeholder="Add a resource"
          value={newResource}
          onChange={setNewResource}
          onAdd={addResource}
        />
      </FormField>

      <FormField label="Topics">
        <TagList
          items={formData.topics}
          onRemove={removeTopic}
          placeholder="Add a topic"
          value={newTopic}
          onChange={setNewTopic}
          onAdd={addTopic}
        />
      </FormField>

      <div className="flex gap-2 justify-end">
        <Button type="button" variant="outline" onClick={onCancel}>
          Cancel
        </Button>
        <Button type="submit" disabled={isLoading}>
          {isLoading ? "Creating..." : "Create Activity"}
        </Button>
      </div>
    </form>
  );
};
