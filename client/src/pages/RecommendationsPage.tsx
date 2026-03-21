import React, { useState, useEffect, useCallback } from "react";
import { useLocation, useSearchParams } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { RecommendationForm } from "@/components/forms/RecommendationForm";
import { ResultsDisplay } from "@/components/results/ResultsDisplay";
import { LoadingState, SkeletonGrid } from "@/components/ui/LoadingState";
import { ErrorDisplay } from "@/components/ui/ErrorDisplay";
import { useDataFetch } from "@/hooks/useDataFetch";
import { useForm } from "@/hooks/useForm";
import { apiService } from "@/services/apiService";
import { convertSearchCriteriaToFormData } from "@/utils/searchCriteriaConverter";
import { ArrowLeft } from "lucide-react";
import { cn } from "@/lib/utils";
import type { ResultsData } from "@/types/activity";
import { useTranslation } from "react-i18next";

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

const initialFormData: FormData = {
  targetAge: 6,
  format: [],
  resourcesNeeded: [],
  bloomLevels: [],
  targetDuration: 60,
  topics: [],
  allowLessonPlans: true,
  maxActivityCount: 2,
  includeBreaks: false,
  priorityCategories: [],
};

export const RecommendationsPage: React.FC = () => {
  const location = useLocation();
  const [searchParams, setSearchParams] = useSearchParams();
  const [, setActiveTab] = useState<"form" | "results">("form");
  const [results, setResults] = useState<ResultsData | null>(null);
  const searchParamsString = searchParams.toString();
  const { t } = useTranslation();

  const getInitialFormData = () => {
    const state = location.state as {
      searchCriteria?: Record<string, unknown>;
    } | null;
    if (state?.searchCriteria) {
      return convertSearchCriteriaToFormData(state.searchCriteria);
    }
    return initialFormData;
  };

  const form = useForm<Partial<FormData>>({
    initialValues: getInitialFormData(),
    onSubmit: async (formData) => {
      const params = new URLSearchParams();

      params.append("targetAge", String(formData.targetAge ?? 6));
      params.append("targetDuration", String(formData.targetDuration ?? 60));
      params.append(
        "allowLessonPlans",
        String(formData.allowLessonPlans ?? true),
      );
      params.append("maxActivityCount", String(formData.maxActivityCount ?? 2));
      params.append("includeBreaks", String(formData.includeBreaks ?? false));

      if ((formData.format?.length ?? 0) > 0) {
        (formData.format as string[]).forEach((f) =>
          params.append("format", f),
        );
      }
      if ((formData.resourcesNeeded?.length ?? 0) > 0) {
        (formData.resourcesNeeded as string[]).forEach((r) =>
          params.append("availableResources", r),
        );
      }
      if ((formData.bloomLevels?.length ?? 0) > 0) {
        (formData.bloomLevels as string[]).forEach((b) =>
          params.append("bloomLevels", b),
        );
      }
      if ((formData.topics?.length ?? 0) > 0) {
        (formData.topics as string[]).forEach((t) =>
          params.append("preferredTopics", t),
        );
      }

      (formData.priorityCategories ?? []).forEach((category) =>
        params.append("priorityCategories", category),
      );

      params.set("limit", "5");

      const data = await apiService.getRecommendations(params.toString());
      setResults(data);
      setActiveTab("results");
    },
  });

  const fetchRecommendations = useCallback(async () => {
    if (!searchParamsString) return null;

    const params = new URLSearchParams(searchParamsString);
    params.set("limit", "5");

    return await apiService.getRecommendations(params.toString());
  }, [searchParamsString]);

  const handleFetchSuccess = useCallback((data: ResultsData | null) => {
    if (data) {
      setResults(data);
      setActiveTab("results");
    }
  }, []);

  const { isLoading, error, refetch } = useDataFetch({
    fetchFn: fetchRecommendations,
    enabled: !!searchParamsString,
    dependencies: [fetchRecommendations],
    onSuccess: handleFetchSuccess,
  });

  const handleFormSubmit = async (formData: FormData) => {
    const params = new URLSearchParams();
    params.append("targetAge", formData.targetAge.toString());
    params.append("targetDuration", formData.targetDuration.toString());
    params.append("allowLessonPlans", formData.allowLessonPlans.toString());
    const effectiveMaxActivities = formData.allowLessonPlans
      ? formData.maxActivityCount
      : 1;
    const effectiveIncludeBreaks = formData.allowLessonPlans
      ? formData.includeBreaks
      : false;
    params.append("maxActivityCount", effectiveMaxActivities.toString());
    params.append("includeBreaks", effectiveIncludeBreaks.toString());
    if (formData.format.length > 0)
      params.append("format", formData.format.join(","));
    if (formData.resourcesNeeded.length > 0)
      params.append("availableResources", formData.resourcesNeeded.join(","));
    if (formData.bloomLevels.length > 0)
      params.append("bloomLevels", formData.bloomLevels.join(","));
    if (formData.topics.length > 0)
      params.append("preferredTopics", formData.topics.join(","));
    formData.priorityCategories.forEach((category) =>
      params.append("priorityCategories", category),
    );
    params.set("limit", "5");
    const data = await apiService.getRecommendations(params.toString());
    setResults(data);
    setActiveTab("results");
  };

  useEffect(() => {
    const hasResults = searchParamsString || results;
    if (hasResults) setActiveTab("results");
  }, [searchParamsString, results]);

  const handleBackToForm = () => {
    setResults(null);
    setActiveTab("form");
    if (searchParamsString) setSearchParams({});
  };

  const showResults = !!(results || (isLoading && !!searchParamsString));

  return (
    <div className="py-6">
      <div className="space-y-8">
        <div className="flex items-start gap-4 sm:gap-6">
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-3">
              {showResults && (
                <Button
                  variant="ghost"
                  size="icon"
                  onClick={handleBackToForm}
                  aria-label={t("recommendations.backToForm")}
                  className="h-9 w-9 flex-shrink-0"
                >
                  <ArrowLeft className="h-5 w-5" />
                </Button>
              )}
              <h1 className="text-2xl sm:text-3xl font-bold tracking-tight text-foreground">
                {t("recommendations.title")}
              </h1>
            </div>
            <p
              className={cn(
                "text-muted-foreground mt-1.5 text-sm sm:text-base",
                showResults && "ml-12",
              )}
            >
              {t("recommendations.subtitle")}
            </p>
          </div>
        </div>

        <ErrorDisplay error={error} onRetry={refetch} />

        {showResults ? (
          <div className="space-y-6">
            <LoadingState isLoading={isLoading} fallback={<SkeletonGrid />}>
              {results && <ResultsDisplay results={results} />}
            </LoadingState>
          </div>
        ) : (
          <div className="mt-8">
            <ErrorDisplay
              error={(form.errors as Record<string, string>).general}
            />
            <RecommendationForm
              onSubmit={handleFormSubmit}
              isLoading={form.isSubmitting}
              initialValues={form.values as FormData}
            />
          </div>
        )}
      </div>
    </div>
  );
};
