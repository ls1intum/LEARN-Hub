import React, { useState, useEffect } from "react";
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
import type { ResultsData } from "@/types/activity";

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
  priority_categories: string[];
}

const initialFormData: FormData = {
  target_age: 6,
  format: [],
  resources_needed: [],
  bloom_levels: [],
  target_duration: 60,
  topics: [],
  allow_lesson_plans: true,
  max_activity_count: 2,
  include_breaks: false,
  priority_categories: [],
};

export const RecommendationsPage: React.FC = () => {
  const location = useLocation();
  const [searchParams, setSearchParams] = useSearchParams();
  const [, setActiveTab] = useState<"form" | "results">("form");
  const [results, setResults] = useState<ResultsData | null>(null);

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

      params.append("target_age", String(formData.target_age ?? 6));
      params.append("target_duration", String(formData.target_duration ?? 60));
      params.append(
        "allow_lesson_plans",
        String(formData.allow_lesson_plans ?? true),
      );
      params.append(
        "max_activity_count",
        String(formData.max_activity_count ?? 2),
      );
      params.append("include_breaks", String(formData.include_breaks ?? false));

      if ((formData.format?.length ?? 0) > 0) {
        params.append("format", (formData.format as string[]).join(","));
      }
      if ((formData.resources_needed?.length ?? 0) > 0) {
        params.append(
          "available_resources",
          (formData.resources_needed as string[]).join(","),
        );
      }
      if ((formData.bloom_levels?.length ?? 0) > 0) {
        params.append("bloom_levels", (formData.bloom_levels as string[]).join(","));
      }
      if ((formData.topics?.length ?? 0) > 0) {
        params.append("preferred_topics", (formData.topics as string[]).join(","));
      }

      (formData.priority_categories ?? []).forEach((category) =>
        params.append("priority_categories", category),
      );

      params.set("limit", "5");

      const data = await apiService.getRecommendations(params.toString());
      setResults(data);
      setActiveTab("results");
    },
  });

  const { isLoading, error, refetch } = useDataFetch({
    fetchFn: async () => {
      if (!searchParams.toString()) return null;

      const params = new URLSearchParams(searchParams.toString());
      params.set("limit", "5");

      return await apiService.getRecommendations(params.toString());
    },
    enabled: !!searchParams.toString(),
    onSuccess: (data) => {
      if (data) {
        setResults(data);
        setActiveTab("results");
      }
    },
  });

  const handleFormSubmit = async (formData: FormData) => {
    const params = new URLSearchParams();
    params.append("target_age", formData.target_age.toString());
    params.append("target_duration", formData.target_duration.toString());
    params.append("allow_lesson_plans", formData.allow_lesson_plans.toString());
    const effectiveMaxActivities = formData.allow_lesson_plans ? formData.max_activity_count : 1;
    const effectiveIncludeBreaks = formData.allow_lesson_plans ? formData.include_breaks : false;
    params.append("max_activity_count", effectiveMaxActivities.toString());
    params.append("include_breaks", effectiveIncludeBreaks.toString());
    if (formData.format.length > 0) params.append("format", formData.format.join(","));
    if (formData.resources_needed.length > 0)
      params.append("available_resources", formData.resources_needed.join(","));
    if (formData.bloom_levels.length > 0)
      params.append("bloom_levels", formData.bloom_levels.join(","));
    if (formData.topics.length > 0)
      params.append("preferred_topics", formData.topics.join(","));
    formData.priority_categories.forEach((category) => params.append("priority_categories", category));
    params.set("limit", "5");
    const data = await apiService.getRecommendations(params.toString());
    setResults(data);
    setActiveTab("results");
  };

  useEffect(() => {
    const hasResults = searchParams.toString() || results;
    if (hasResults) setActiveTab("results");
  }, [searchParams, results]);

  const handleBackToForm = () => {
    setResults(null);
    setActiveTab("form");
    if (searchParams.toString()) setSearchParams({});
  };

  return (
    <div className="container mx-auto py-8 max-w-6xl">
      <div className="space-y-8">
        <div className="flex items-start gap-4 sm:gap-6">
          <div className="flex-1 min-w-0">
            <h1 className="text-2xl sm:text-3xl font-bold text-foreground mb-2">Activity Recommendations</h1>
            <p className="text-muted-foreground text-base sm:text-lg">Get personalized activity recommendations for your teaching needs</p>
          </div>
        </div>

        <ErrorDisplay error={error} onRetry={refetch} />

        {results || (isLoading && !!searchParams.toString()) ? (
          <div className="mt-8 space-y-6">
            <div>
              <Button variant="ghost" onClick={handleBackToForm}>
                <ArrowLeft className="h-4 w-4 mr-2" />
                Back to form
              </Button>
            </div>
            <LoadingState isLoading={isLoading} fallback={<SkeletonGrid />}>
              {results && <ResultsDisplay results={results} />}
            </LoadingState>
          </div>
        ) : (
          <div className="mt-8">
            <ErrorDisplay error={(form.errors as Record<string, string>).general} />
            <RecommendationForm onSubmit={handleFormSubmit} isLoading={form.isSubmitting} initialValues={form.values as FormData} />
          </div>
        )}
      </div>
    </div>
  );
};
