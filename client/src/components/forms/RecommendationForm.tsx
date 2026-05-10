import React, { useEffect, useState, useCallback, useRef } from "react";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Sparkles, ArrowLeft, ArrowRight, Info, CheckCircle } from "lucide-react";
import { useFieldValues } from "@/hooks/useFieldValues";
import { BadgeSelector } from "@/components/ui/BadgeSelector";
import { RangeSlider } from "@/components/ui/RangeSlider";
import { PriorityToggle } from "@/components/ui/PriorityToggle";
import { useTranslation } from "react-i18next";
import { useTranslateEnum } from "@/hooks/useTranslateEnum";
import { cn } from "@/lib/utils";

interface FormData {
  targetAge: number;
  format: string[];
  resourcesNeeded: string[];
  bloomLevels: string[];
  targetDuration: number;
  topics: string[];
  allowLessonPlans: boolean;
  maxActivityCount: number;
  includeBreaks: boolean;
  priorityCategories: string[];
}

interface RecommendationFormProps {
  onSubmit: (formData: FormData) => void;
  isLoading?: boolean;
  initialValues?: Partial<FormData>;
}

const TOTAL_STEPS = 7;

export const RecommendationForm: React.FC<RecommendationFormProps> = ({
  onSubmit,
  isLoading = false,
  initialValues,
}) => {
  const { fieldValues } = useFieldValues();
  const { t } = useTranslation();
  const translateEnum = useTranslateEnum();
  const containerRef = useRef<HTMLDivElement>(null);

  const [currentStep, setCurrentStep] = useState(0);
  const [isMaxActivityCountManuallyTouched, setIsMaxActivityCountManuallyTouched] = useState(false);

  const [formData, setFormData] = useState<FormData>({
    targetAge: initialValues?.targetAge ?? 10,
    format: initialValues?.format ?? [],
    resourcesNeeded: initialValues?.resourcesNeeded ?? [],
    bloomLevels: initialValues?.bloomLevels ?? [],
    targetDuration: initialValues?.targetDuration ?? 60,
    topics: initialValues?.topics ?? [],
    allowLessonPlans: initialValues?.allowLessonPlans ?? false,
    maxActivityCount: initialValues?.maxActivityCount ?? 2,
    includeBreaks: initialValues?.includeBreaks ?? false,
    priorityCategories: initialValues?.priorityCategories ?? [],
  });

  const initialMaxActivityCount =
    initialValues?.maxActivityCount ??
    Math.max(1, Math.floor((initialValues?.targetDuration ?? 60) / 30));

  useEffect(() => {
    if (!initialValues?.maxActivityCount) {
      setFormData((prev) => ({ ...prev, maxActivityCount: initialMaxActivityCount }));
    }
  }, [initialMaxActivityCount, initialValues?.maxActivityCount]);

  useEffect(() => {
    if (fieldValues) {
      setFormData((prev) => ({
        ...prev,
        format: prev.format.length > 0 ? prev.format : fieldValues.format || [],
        resourcesNeeded:
          prev.resourcesNeeded.length > 0 ? prev.resourcesNeeded : fieldValues.resourcesAvailable || [],
        bloomLevels:
          prev.bloomLevels.length > 0 ? prev.bloomLevels : fieldValues.bloomLevel || [],
        topics: prev.topics.length > 0 ? prev.topics : fieldValues.topics || [],
      }));
    }
  }, [fieldValues]);

  const actualFormat = fieldValues?.format || [];
  const actualResources = fieldValues?.resourcesAvailable || [];
  const actualBloomLevels = fieldValues?.bloomLevel || [];
  const actualTopics = fieldValues?.topics || [];

  const updateFormData = (updates: Partial<FormData>) => {
    setFormData((prev) => {
      const newData = { ...prev, ...updates };
      if (updates.targetDuration !== undefined && !isMaxActivityCountManuallyTouched) {
        newData.maxActivityCount = Math.max(1, Math.floor(updates.targetDuration / 30));
      }
      return newData;
    });
  };

  const toggleArrayValue = (field: keyof FormData, value: string) => {
    const currentArray = formData[field] as string[];
    const multiSelectFields = ["format", "resourcesNeeded", "bloomLevels", "topics"];
    if (currentArray.includes(value)) {
      if (multiSelectFields.includes(field) && currentArray.length === 1) return;
      updateFormData({ [field]: currentArray.filter((item) => item !== value) });
    } else {
      updateFormData({ [field]: [...currentArray, value] });
    }
  };

  const togglePriorityCategory = (category: string) => {
    const current = formData.priorityCategories;
    updateFormData({
      priorityCategories: current.includes(category)
        ? current.filter((c) => c !== category)
        : [...current, category],
    });
  };

  const isPriorityCategory = (category: string) => formData.priorityCategories.includes(category);

  const isLastStep = currentStep === TOTAL_STEPS - 1;

  const handleNext = useCallback(() => {
    if (isLastStep) {
      onSubmit(formData);
    } else {
      setCurrentStep((s) => Math.min(s + 1, TOTAL_STEPS - 1));
    }
  }, [isLastStep, formData, onSubmit]);

  const handleBack = () => setCurrentStep((s) => Math.max(s - 1, 0));

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (
        e.key === "Enter" &&
        !(e.target as HTMLElement).closest('button, input[type="checkbox"]')
      ) {
        e.preventDefault();
        handleNext();
      }
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [handleNext]);

  const getAnswerSummary = (stepIndex: number): string => {
    const summarizeArray = (items: string[], all: string[], translateFn: (v: string) => string) => {
      if (all.length > 0 && items.length === all.length) return t("recommendationFormGuided.allSelected");
      return items.map(translateFn).join(", ") || "—";
    };
    switch (stepIndex) {
      case 0:
        return `${formData.targetAge}${t("recommendationForm.unitYears")}`;
      case 1:
        return `${formData.targetDuration}${t("recommendationForm.unitMinutes")}`;
      case 2:
        return summarizeArray(formData.format, actualFormat, (v) => translateEnum("format", v));
      case 3:
        return summarizeArray(formData.resourcesNeeded, actualResources, (v) => translateEnum("resources", v));
      case 4:
        return summarizeArray(formData.bloomLevels, actualBloomLevels, (v) => translateEnum("bloomLevel", v));
      case 5:
        return summarizeArray(formData.topics, actualTopics, (v) => translateEnum("topics", v));
      case 6:
        return formData.allowLessonPlans
          ? t("recommendationFormGuided.lessonPlanEnabled")
          : t("recommendationFormGuided.singleActivity");
      default:
        return "—";
    }
  };

  const steps = [
    {
      category: t("recommendationFormGuided.step1Category"),
      question: t("recommendationFormGuided.step1Question"),
      description: t("recommendationFormGuided.step1Description"),
      tip: t("recommendationFormGuided.step1Tip"),
      sidebar: t("recommendationFormGuided.step1Sidebar"),
    },
    {
      category: t("recommendationFormGuided.step2Category"),
      question: t("recommendationFormGuided.step2Question"),
      description: t("recommendationFormGuided.step2Description"),
      tip: "",
      sidebar: t("recommendationFormGuided.step2Sidebar"),
    },
    {
      category: t("recommendationFormGuided.step3Category"),
      question: t("recommendationFormGuided.step3Question"),
      description: t("recommendationFormGuided.step3Description"),
      tip: "",
      sidebar: t("recommendationFormGuided.step3Sidebar"),
    },
    {
      category: t("recommendationFormGuided.step4Category"),
      question: t("recommendationFormGuided.step4Question"),
      description: t("recommendationFormGuided.step4Description"),
      tip: t("recommendationFormGuided.step4Tip"),
      sidebar: t("recommendationFormGuided.step4Sidebar"),
    },
    {
      category: t("recommendationFormGuided.step5Category"),
      question: t("recommendationFormGuided.step5Question"),
      description: t("recommendationFormGuided.step5Description"),
      tip: "",
      sidebar: t("recommendationFormGuided.step5Sidebar"),
    },
    {
      category: t("recommendationFormGuided.step6Category"),
      question: t("recommendationFormGuided.step6Question"),
      description: t("recommendationFormGuided.step6Description"),
      tip: t("recommendationFormGuided.step6Tip"),
      sidebar: t("recommendationFormGuided.step6Sidebar"),
    },
    {
      category: t("recommendationFormGuided.step7Category"),
      question: t("recommendationFormGuided.step7Question"),
      description: t("recommendationFormGuided.step7Description"),
      tip: "",
      sidebar: t("recommendationFormGuided.step7Sidebar"),
    },
  ];

  const step = steps[currentStep];

  const renderStepInput = () => {
    switch (currentStep) {
      case 0:
        return (
          <RangeSlider
            label={t("recommendationForm.ageAppropriateness")}
            value={formData.targetAge}
            min={6}
            max={15}
            step={1}
            onChange={(value) => updateFormData({ targetAge: value })}
            unit={t("recommendationForm.unitYears")}
            priorityToggle={
              <PriorityToggle
                category="age_appropriateness"
                isPriority={isPriorityCategory("age_appropriateness")}
                onToggle={togglePriorityCategory}
              />
            }
          />
        );
      case 1:
        return (
          <RangeSlider
            label={t("recommendationForm.durationFit")}
            value={formData.targetDuration}
            min={15}
            max={180}
            step={15}
            onChange={(value) => updateFormData({ targetDuration: value })}
            unit={t("recommendationForm.unitMinutes")}
            priorityToggle={
              <PriorityToggle
                category="duration_fit"
                isPriority={isPriorityCategory("duration_fit")}
                onToggle={togglePriorityCategory}
              />
            }
          />
        );
      case 2:
        return (
          <BadgeSelector
            label={t("recommendationForm.activityFormat")}
            options={actualFormat}
            selectedValues={formData.format}
            onToggle={(value) => toggleArrayValue("format", value)}
            labelFn={(value) => translateEnum("format", value)}
          />
        );
      case 3:
        return (
          <BadgeSelector
            label={t("recommendationForm.resourcesAvailable")}
            options={actualResources}
            selectedValues={formData.resourcesNeeded}
            onToggle={(value) => toggleArrayValue("resourcesNeeded", value)}
            labelFn={(value) => translateEnum("resources", value)}
          />
        );
      case 4:
        return (
          <BadgeSelector
            label={t("recommendationForm.bloomLevelMatch")}
            options={actualBloomLevels}
            selectedValues={formData.bloomLevels}
            onToggle={(value) => toggleArrayValue("bloomLevels", value)}
            labelFn={(value) => translateEnum("bloomLevel", value)}
            priorityToggle={
              <PriorityToggle
                category="bloom_level_match"
                isPriority={isPriorityCategory("bloom_level_match")}
                onToggle={togglePriorityCategory}
              />
            }
          />
        );
      case 5:
        return (
          <BadgeSelector
            label={t("recommendationForm.topicRelevance")}
            options={actualTopics}
            selectedValues={formData.topics}
            onToggle={(value) => toggleArrayValue("topics", value)}
            labelFn={(value) => translateEnum("topics", value)}
            priorityToggle={
              <PriorityToggle
                category="topic_relevance"
                isPriority={isPriorityCategory("topic_relevance")}
                onToggle={togglePriorityCategory}
              />
            }
          />
        );
      case 6:
        return (
          <div className="space-y-4">
            <div className="flex items-center justify-between rounded-lg border bg-card p-4">
              <div className="space-y-0.5">
                <Label className="text-base font-medium">
                  {t("recommendationForm.allowLessonPlans")}
                </Label>
                <p className="text-sm text-muted-foreground">
                  {t("recommendationForm.allowLessonPlansDesc")}
                </p>
              </div>
              <input
                type="checkbox"
                checked={formData.allowLessonPlans}
                onChange={(e) => updateFormData({ allowLessonPlans: e.target.checked })}
                className="h-5 w-5 accent-primary"
              />
            </div>
            {formData.allowLessonPlans && (
              <>
                <RangeSlider
                  label={t("recommendationForm.maxActivities")}
                  value={formData.maxActivityCount}
                  min={1}
                  max={5}
                  step={1}
                  onChange={(value) => {
                    setIsMaxActivityCountManuallyTouched(true);
                    updateFormData({ maxActivityCount: value });
                  }}
                />
                <div className="flex items-center justify-between rounded-lg border bg-card p-4">
                  <div className="space-y-0.5">
                    <Label className="text-base font-medium">
                      {t("recommendationForm.includeBreaks")}
                    </Label>
                    <p className="text-sm text-muted-foreground">
                      {t("recommendationForm.includeBreaksDesc")}
                    </p>
                  </div>
                  <input
                    type="checkbox"
                    checked={formData.includeBreaks}
                    onChange={(e) => updateFormData({ includeBreaks: e.target.checked })}
                    className="h-5 w-5 accent-primary"
                  />
                </div>
              </>
            )}
          </div>
        );
      default:
        return null;
    }
  };

  return (
    <div ref={containerRef} className="min-h-[480px]">
      {/* Mobile step counter */}
      <div className="lg:hidden mb-6 flex items-center gap-3">
        <div className="flex-1 h-1.5 rounded-full bg-muted overflow-hidden">
          <div
            className="h-full bg-primary rounded-full transition-all duration-300"
            style={{ width: `${((currentStep + 1) / TOTAL_STEPS) * 100}%` }}
          />
        </div>
        <span className="text-sm text-muted-foreground flex-shrink-0">
          {currentStep + 1} / {TOTAL_STEPS}
        </span>
      </div>

      {/* Two-column layout */}
      <div className="grid grid-cols-1 lg:grid-cols-[1fr_300px] gap-8 lg:gap-14">
        {/* Main content */}
        <div className="flex flex-col">
          <p className="text-xs font-semibold uppercase tracking-widest text-primary mb-3">
            {step.category}
          </p>
          <h2 className="text-2xl sm:text-3xl font-bold tracking-tight text-foreground mb-4">
            {step.question}
          </h2>
          <p className="text-muted-foreground leading-relaxed mb-8 max-w-xl">
            {step.description}
          </p>

          <div className="flex-1">{renderStepInput()}</div>

          {step.tip && (
            <div className="mt-6 flex items-start gap-2.5 text-sm text-muted-foreground">
              <Info className="h-4 w-4 text-primary/70 mt-0.5 flex-shrink-0" />
              <span>
                <span className="font-semibold text-foreground">
                  {t("recommendationFormGuided.tip")}
                </span>
                {" · "}
                {step.tip}
              </span>
            </div>
          )}
        </div>

        {/* Answers sidebar */}
        <div className="hidden lg:block">
          <div className="rounded-xl border bg-card p-5 sticky top-8">
            <p className="text-xs font-semibold uppercase tracking-widest text-muted-foreground mb-5">
              {t("recommendationFormGuided.yourAnswers")}
            </p>
            <ol className="space-y-1">
              {steps.map((s, idx) => (
                <li
                  key={idx}
                  className={cn(
                    "flex items-start gap-3 rounded-lg px-2 py-1.5 -mx-2 transition-colors",
                    idx <= currentStep && "cursor-pointer hover:bg-muted/50",
                    idx > currentStep && "opacity-40 cursor-default"
                  )}
                  onClick={() => idx <= currentStep && setCurrentStep(idx)}
                >
                  <div
                    className={cn(
                      "w-6 h-6 rounded-full flex items-center justify-center text-xs font-semibold flex-shrink-0 mt-0.5 transition-all",
                      idx < currentStep && "bg-primary text-primary-foreground",
                      idx === currentStep &&
                      "bg-primary text-primary-foreground ring-2 ring-primary/25 ring-offset-1 ring-offset-card",
                      idx > currentStep && "bg-muted text-muted-foreground border border-border"
                    )}
                  >
                    {idx < currentStep ? <CheckCircle className="h-3.5 w-3.5" /> : idx + 1}
                  </div>
                  <div className="min-w-0 flex-1 min-h-[2rem]">
                    <p
                      className={cn(
                        "line-clamp-2 text-sm font-medium leading-tight",
                        idx === currentStep ? "text-foreground" : "text-muted-foreground"
                      )}
                    >
                      {s.sidebar}
                    </p>
                    <p
                      className={cn(
                        "mt-0.5 min-h-[1rem] line-clamp-1 text-xs",
                        idx < currentStep && "font-medium text-primary",
                        idx === currentStep && "text-muted-foreground",
                        idx > currentStep && "invisible"
                      )}
                    >
                      {idx <= currentStep ? getAnswerSummary(idx) : "—"}
                    </p>
                  </div>
                </li>
              ))}
            </ol>
          </div>
        </div>
      </div>

      {/* Navigation */}
      <div className="mt-10 pt-6 border-t flex items-center justify-between gap-4">
        <Button
          type="button"
          variant="ghost"
          onClick={handleBack}
          disabled={currentStep === 0}
          className="gap-1.5"
        >
          <ArrowLeft className="h-4 w-4" />
          {t("recommendationFormGuided.back")}
        </Button>

        <p className="hidden sm:flex items-center gap-1.5 text-sm text-muted-foreground select-none">
          {t("recommendationFormGuided.pressEnterPrefix")}
          <kbd className="inline-flex items-center rounded border border-border bg-muted px-1.5 py-0.5 font-mono text-xs">
            Enter
          </kbd>
          {t("recommendationFormGuided.pressEnterSuffix")}
        </p>

        <Button
          type="button"
          onClick={handleNext}
          disabled={isLoading}
          className={cn(
            "gap-2 min-w-[140px]",
            isLastStep && "shadow-md hover:shadow-lg transition-all duration-300"
          )}
        >
          {isLoading ? (
            <>
              <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white" />
              {t("recommendationForm.generating")}
            </>
          ) : isLastStep ? (
            <>
              <Sparkles className="h-4 w-4" />
              {t("recommendationForm.getRecommendations")}
            </>
          ) : (
            <>
              {t("recommendationFormGuided.continue")}
              <ArrowRight className="h-4 w-4" />
            </>
          )}
        </Button>
      </div>
    </div>
  );
};
