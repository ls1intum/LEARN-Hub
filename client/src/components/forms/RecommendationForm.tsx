import React, { useEffect, useState } from "react";
import { Button } from "@/components/ui/button";
import { Separator } from "@/components/ui/separator";
import { Label } from "@/components/ui/label";
import { Sparkles, Target, BookOpen, Settings } from "lucide-react";
import { useFieldValues } from "@/hooks/useFieldValues";
import { BadgeSelector } from "@/components/ui/BadgeSelector";
import { FormSection } from "@/components/ui/FormSection";
import { RangeSlider } from "@/components/ui/RangeSlider";
import { PriorityToggle } from "@/components/ui/PriorityToggle";

interface FormData {
  target_age: number;
  format: string[];
  resources_needed: string[];
  bloom_levels: string[];
  target_duration: number;
  topics: string[];
  allow_lesson_plans: boolean;
  max_activity_count: number;
  include_breaks: boolean;
  priority_categories: string[]; // Categories to prioritize in scoring
}

interface RecommendationFormProps {
  onSubmit: (formData: FormData) => void;
  isLoading?: boolean;
  initialValues?: Partial<FormData>;
}

// Note: defaultFormData would be used when fully integrating useForm hook
// const defaultFormData: FormData = { ... };

export const RecommendationForm: React.FC<RecommendationFormProps> = ({
  onSubmit,
  isLoading = false,
  initialValues,
}) => {
  const { fieldValues } = useFieldValues();

  // Note: This form is still using the old state management approach
  // The useForm hook is imported but not yet fully integrated
  // This is a complex form that would benefit from gradual refactoring

  // Initialize form data with default values
  const [formData, setFormData] = useState<FormData>({
    target_age: initialValues?.target_age ?? 10,
    format: initialValues?.format ?? [],
    resources_needed: initialValues?.resources_needed ?? [],
    bloom_levels: initialValues?.bloom_levels ?? [],
    target_duration: initialValues?.target_duration ?? 60,
    topics: initialValues?.topics ?? [],
    allow_lesson_plans: initialValues?.allow_lesson_plans ?? false,
    max_activity_count: initialValues?.max_activity_count ?? 2,
    include_breaks: initialValues?.include_breaks ?? false,
    priority_categories: initialValues?.priority_categories ?? [],
  });

  // Track if max_activity_count has been manually touched
  const [
    isMaxActivityCountManuallyTouched,
    setIsMaxActivityCountManuallyTouched,
  ] = useState(false);

  // Calculate initial max_activity_count based on duration if not provided
  const initialMaxActivityCount =
    initialValues?.max_activity_count ??
    Math.max(1, Math.floor((initialValues?.target_duration ?? 60) / 30));

  // Update formData to use calculated initial value
  useEffect(() => {
    if (!initialValues?.max_activity_count) {
      setFormData((prev) => ({
        ...prev,
        max_activity_count: initialMaxActivityCount,
      }));
    }
  }, [initialMaxActivityCount, initialValues?.max_activity_count]);

  // Initialize multi-select fields with all options when fieldValues are available
  useEffect(() => {
    if (fieldValues) {
      setFormData((prev) => ({
        ...prev,
        format: prev.format.length > 0 ? prev.format : fieldValues.format || [],
        resources_needed:
          prev.resources_needed.length > 0
            ? prev.resources_needed
            : fieldValues.resources_available || [],
        bloom_levels:
          prev.bloom_levels.length > 0
            ? prev.bloom_levels
            : fieldValues.bloom_level || [],
        topics: prev.topics.length > 0 ? prev.topics : fieldValues.topics || [],
      }));
    }
  }, [fieldValues]);

  // Get all available options to display - always show all options regardless of selection
  const actualFormat = fieldValues?.format || [];
  const actualResources = fieldValues?.resources_available || [];
  const actualBloomLevels = fieldValues?.bloom_level || [];
  const actualTopics = fieldValues?.topics || [];

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSubmit(formData);
  };

  const updateFormData = (updates: Partial<FormData>) => {
    setFormData((prev) => {
      const newData = { ...prev, ...updates };

      // If duration is being updated and max_activity_count hasn't been manually touched,
      // automatically calculate max_activity_count using the formula: duration // 30
      if (
        updates.target_duration !== undefined &&
        !isMaxActivityCountManuallyTouched
      ) {
        newData.max_activity_count = Math.max(
          1,
          Math.floor(updates.target_duration / 30),
        );
      }

      return newData;
    });
  };

  const toggleArrayValue = (field: keyof FormData, value: string) => {
    const currentArray = formData[field] as string[];

    // Check if this is a multi-select field that requires at least one selection
    const multiSelectFields = [
      "format",
      "resources_needed",
      "bloom_levels",
      "topics",
    ];
    const isMultiSelectField = multiSelectFields.includes(field);

    if (currentArray.includes(value)) {
      // Trying to deselect - check if this would leave the array empty
      if (isMultiSelectField && currentArray.length === 1) {
        // Don't allow deselecting the last item
        return;
      }
      const newArray = currentArray.filter((item) => item !== value);
      updateFormData({ [field]: newArray });
    } else {
      // Adding a new selection
      const newArray = [...currentArray, value];
      updateFormData({ [field]: newArray });
    }
  };

  const handleMaxActivityCountChange = (value: number) => {
    setIsMaxActivityCountManuallyTouched(true);
    updateFormData({ max_activity_count: value });
  };

  const togglePriorityCategory = (category: string) => {
    const currentCategories = formData.priority_categories;
    const newCategories = currentCategories.includes(category)
      ? currentCategories.filter((c) => c !== category)
      : [...currentCategories, category];
    updateFormData({ priority_categories: newCategories });
  };

  const isPriorityCategory = (category: string) => {
    return formData.priority_categories.includes(category);
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      {/* Basic Requirements */}
      <FormSection title="Basic Requirements" icon={Target}>
        {/* Target Age */}
        <RangeSlider
          label="Age Appropriateness"
          value={formData.target_age}
          min={6}
          max={15}
          step={1}
          onChange={(value) => updateFormData({ target_age: value })}
          unit=" years"
          priorityToggle={
            <PriorityToggle
              category="age_appropriateness"
              isPriority={isPriorityCategory("age_appropriateness")}
              onToggle={togglePriorityCategory}
            />
          }
        />

        {/* Target Duration */}
        <RangeSlider
          label="Duration Fit"
          value={formData.target_duration}
          min={15}
          max={180}
          step={15}
          onChange={(value) => updateFormData({ target_duration: value })}
          unit=" minutes"
          priorityToggle={
            <PriorityToggle
              category="duration_fit"
              isPriority={isPriorityCategory("duration_fit")}
              onToggle={togglePriorityCategory}
            />
          }
        />

        {/* Format Selection */}
        <BadgeSelector
          label="Activity Format"
          options={actualFormat}
          selectedValues={formData.format}
          onToggle={(value) => toggleArrayValue("format", value)}
        />

        {/* Resources Needed */}
        <BadgeSelector
          label="Resources Available"
          options={actualResources}
          selectedValues={formData.resources_needed}
          onToggle={(value) => toggleArrayValue("resources_needed", value)}
        />
      </FormSection>

      {/* Learning Objectives */}
      <FormSection title="Learning Objectives" icon={BookOpen}>
        {/* Bloom's Taxonomy Levels */}
        <BadgeSelector
          label="Bloom Level Match"
          options={actualBloomLevels}
          selectedValues={formData.bloom_levels}
          onToggle={(value) => toggleArrayValue("bloom_levels", value)}
          priorityToggle={
            <PriorityToggle
              category="bloom_level_match"
              isPriority={isPriorityCategory("bloom_level_match")}
              onToggle={togglePriorityCategory}
            />
          }
        />

        {/* Topics */}
        <BadgeSelector
          label="Topic Relevance"
          options={actualTopics}
          selectedValues={formData.topics}
          onToggle={(value) => toggleArrayValue("topics", value)}
          priorityToggle={
            <PriorityToggle
              category="topic_relevance"
              isPriority={isPriorityCategory("topic_relevance")}
              onToggle={togglePriorityCategory}
            />
          }
        />
      </FormSection>

      {/* Advanced Options */}
      <FormSection title="Advanced Options" icon={Settings}>
        <div className="flex items-center justify-between">
          <div className="space-y-0.5">
            <Label className="text-base font-medium">Allow Lesson Plans</Label>
            <p className="text-sm text-muted-foreground">
              Generate structured lesson plans with multiple activities
            </p>
          </div>
          <input
            type="checkbox"
            checked={formData.allow_lesson_plans}
            onChange={(e) =>
              updateFormData({ allow_lesson_plans: e.target.checked })
            }
          />
        </div>

        {formData.allow_lesson_plans && (
          <>
            <RangeSlider
              label="Max Activities"
              value={formData.max_activity_count}
              min={1}
              max={5}
              step={1}
              onChange={handleMaxActivityCountChange}
            />

            <div className="flex items-center justify-between">
              <div className="space-y-0.5">
                <Label className="text-base font-medium">Include Breaks</Label>
                <p className="text-sm text-muted-foreground">
                  Add break recommendations between activities
                </p>
              </div>
              <input
                type="checkbox"
                checked={formData.include_breaks}
                onChange={(e) =>
                  updateFormData({ include_breaks: e.target.checked })
                }
              />
            </div>
          </>
        )}
      </FormSection>

      <Separator className="my-6" />

      <div className="flex justify-end">
        <Button
          type="submit"
          disabled={isLoading}
          className="min-w-[140px] bg-gradient-to-r from-primary to-primary/80 hover:from-primary/90 hover:to-primary/70 shadow-md hover:shadow-lg transition-all duration-300"
        >
          {isLoading ? (
            <>
              <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white mr-2" />
              Generating...
            </>
          ) : (
            <>
              <Sparkles className="h-4 w-4 mr-2" />
              Get Recommendations
            </>
          )}
        </Button>
      </div>
    </form>
  );
};
