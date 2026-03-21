import React, { useEffect, useState } from "react";
import { RecommendationRow } from "@/components/RecommendationRow";
import { LessonPlanModal } from "@/components/LessonPlanModal";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Slider } from "@/components/ui/slider";
import {
  AlertCircle,
  Clock,
  Filter,
  Search,
  Target,
  Users,
} from "lucide-react";
import type { ResultsData, Recommendation } from "@/types/activity";
import type { LessonPlanData } from "@/types/activity";
import { useTranslation } from "react-i18next";

interface ResultsDisplayProps {
  results: ResultsData;
  className?: string;
}

export const ResultsDisplay: React.FC<ResultsDisplayProps> = ({
  results,
  className = "",
}) => {
  const { t } = useTranslation();
  const hasRecommendations = Boolean(results?.activities?.length);
  const [isLessonPlanOpen, setIsLessonPlanOpen] = useState(false);
  const [lessonPlanData, setLessonPlanData] = useState<LessonPlanData | null>(
    null,
  );
  const [searchQuery, setSearchQuery] = useState("");
  const [scoreThreshold, setScoreThreshold] = useState(0);
  const [durationRange, setDurationRange] = useState<[number, number]>([0, 0]);
  const [activityCountRange, setActivityCountRange] = useState<
    [number, number]
  >([1, 1]);
  const [showFilters, setShowFilters] = useState(false);

  const handleCreateLessonPlanFromRecommendation = (
    recommendation: Recommendation,
  ) => {
    // Calculate total duration from activities
    const totalDuration = recommendation.activities.reduce(
      (total, activity) => {
        return total + (activity.durationMinMinutes || 0);
      },
      0,
    );

    const data: LessonPlanData = {
      activities: recommendation.activities,
      totalDurationMinutes: totalDuration,
      breaks: [], // Breaks are now included inline with activities
      ordering_strategy: "balanced",
      title: "My Lesson Plan",
    };

    setLessonPlanData(data);
    setIsLessonPlanOpen(true);
  };

  const handleCloseLessonPlan = () => {
    setIsLessonPlanOpen(false);
    setLessonPlanData(null);
  };

  const recommendationSummaries = (results?.activities ?? []).map(
    (recommendation) => {
      const totalDuration = recommendation.activities.reduce(
        (total, activity) => {
          const activityDuration = activity.durationMinMinutes || 0;
          const breakDuration = activity.breakAfter?.duration || 0;
          return total + activityDuration + breakDuration;
        },
        0,
      );

      const activityCount = recommendation.activities.length;
      const searchText = recommendation.activities
        .flatMap((activity) => [activity.name, activity.description])
        .join(" ")
        .toLowerCase();

      return {
        recommendation,
        totalDuration,
        activityCount,
        searchText,
      };
    },
  );

  const durationBounds = recommendationSummaries.reduce(
    (bounds, item) => ({
      min: Math.min(bounds.min, item.totalDuration),
      max: Math.max(bounds.max, item.totalDuration),
    }),
    { min: Number.POSITIVE_INFINITY, max: 0 },
  );
  const activityCountBounds = recommendationSummaries.reduce(
    (bounds, item) => ({
      min: Math.min(bounds.min, item.activityCount),
      max: Math.max(bounds.max, item.activityCount),
    }),
    { min: Number.POSITIVE_INFINITY, max: 0 },
  );

  const normalizedDurationBounds: [number, number] = [
    Number.isFinite(durationBounds.min) ? durationBounds.min : 0,
    durationBounds.max,
  ];
  const normalizedActivityCountBounds: [number, number] = [
    Number.isFinite(activityCountBounds.min) ? activityCountBounds.min : 1,
    activityCountBounds.max || 1,
  ];
  const [minDuration, maxDuration] = normalizedDurationBounds;
  const [minActivityCount, maxActivityCount] = normalizedActivityCountBounds;

  useEffect(() => {
    setSearchQuery("");
    setScoreThreshold(0);
    setDurationRange([minDuration, maxDuration]);
    setActivityCountRange([minActivityCount, maxActivityCount]);
    setShowFilters(false);
  }, [results, minDuration, maxDuration, minActivityCount, maxActivityCount]);

  const filteredRecommendations = recommendationSummaries.filter((item) => {
    const matchesQuery =
      searchQuery.trim().length === 0 ||
      item.searchText.includes(searchQuery.trim().toLowerCase());

    return (
      matchesQuery &&
      item.recommendation.score >= scoreThreshold &&
      item.totalDuration >= durationRange[0] &&
      item.totalDuration <= durationRange[1] &&
      item.activityCount >= activityCountRange[0] &&
      item.activityCount <= activityCountRange[1]
    );
  });

  const resetFilters = () => {
    setSearchQuery("");
    setScoreThreshold(0);
    setDurationRange(normalizedDurationBounds);
    setActivityCountRange(normalizedActivityCountBounds);
  };

  if (!hasRecommendations) {
    return (
      <div className="text-center py-12">
        <AlertCircle className="h-12 w-12 text-muted-foreground mx-auto mb-4" />
        <h3 className="text-lg font-semibold text-foreground mb-2">
          {t("resultsDisplay.noRecommendations")}
        </h3>
        <p className="text-muted-foreground">
          {t("resultsDisplay.noRecommendationsDesc")}
        </p>
      </div>
    );
  }

  return (
    <>
      <div className={`space-y-8 ${className}`}>
        <Card className="border-border/60 shadow-none">
          <CardContent className="p-4 sm:p-6 lg:p-8">
            <div className="mb-6">
              <div className="flex flex-col sm:flex-row gap-4 mb-4">
                <div className="flex-1">
                  <div className="relative">
                    <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                    <Input
                      id="recommendation-search"
                      value={searchQuery}
                      onChange={(event) => setSearchQuery(event.target.value)}
                      placeholder={t("resultsDisplay.searchPlaceholder")}
                      className="pl-10"
                    />
                  </div>
                </div>
                <div className="flex items-center gap-2">
                  <Button
                    variant="outline"
                    onClick={() => setShowFilters(!showFilters)}
                    className="flex items-center gap-2"
                  >
                    <Filter className="h-4 w-4" />
                    {showFilters ? t("resultsDisplay.hideFilters") : t("resultsDisplay.showFilters")}
                  </Button>
                  <Button variant="outline" onClick={resetFilters}>
                    {t("resultsDisplay.clearFilters")}
                  </Button>
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-8">
                <div className="rounded-xl border border-primary/10 bg-primary/5 p-6">
                  <div className="flex items-center">
                    <div className="rounded-xl bg-primary/15 p-3">
                      <Target className="h-6 w-6 text-primary" />
                    </div>
                    <div className="ml-4">
                      <p className="text-sm font-medium text-primary">
                        {t("resultsDisplay.totalRecommendations")}
                      </p>
                      <p className="text-3xl font-bold text-primary">
                        {results.activities.length}
                      </p>
                    </div>
                  </div>
                </div>
                <div className="rounded-xl border border-success/10 bg-success/5 p-6">
                  <div className="flex items-center">
                    <div className="rounded-xl bg-success/15 p-3">
                      <Filter className="h-6 w-6 text-success" />
                    </div>
                    <div className="ml-4">
                      <p className="text-sm font-medium text-success">
                        {t("resultsDisplay.showing")}
                      </p>
                      <p className="text-3xl font-bold text-success">
                        {filteredRecommendations.length}
                      </p>
                    </div>
                  </div>
                </div>
              </div>

              {showFilters && (
                <div className="mb-8 rounded-xl border border-border/50 bg-gradient-to-br from-muted/20 to-muted/10 p-6 shadow-sm">
                  <div className="mb-8 flex items-center justify-between">
                    <h3 className="text-lg font-semibold text-foreground">
                      {t("resultsDisplay.advancedFilters")}
                    </h3>
                  </div>

                  <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
                    <div className="rounded-lg border border-border/50 bg-card/50 p-5 shadow-xs">
                      <div className="mb-1 flex items-center gap-2">
                        <Target className="h-4 w-4 text-primary" />
                        <Label
                          htmlFor="score-threshold"
                          className="font-semibold"
                        >
                          {t("resultsDisplay.matchPercentage")}
                        </Label>
                      </div>
                      <div className="mb-4 flex items-center justify-between">
                        <span className="text-sm text-muted-foreground">
                          {t("resultsDisplay.minimumScore")}
                        </span>
                        <span className="text-lg font-bold text-primary">
                          {scoreThreshold}%+
                        </span>
                      </div>
                      <Slider
                        id="score-threshold"
                        min={0}
                        max={100}
                        step={1}
                        value={[scoreThreshold]}
                        onValueChange={([value]) =>
                          setScoreThreshold(value ?? 0)
                        }
                        className="w-full"
                      />
                    </div>

                    <div className="rounded-lg border border-border/50 bg-card/50 p-5 shadow-xs">
                      <div className="mb-1 flex items-center gap-2">
                        <Clock className="h-4 w-4 text-primary" />
                        <Label className="font-semibold">
                          {t("resultsDisplay.length")}
                        </Label>
                      </div>
                      <div className="mb-4 flex items-center justify-between">
                        <span className="text-sm text-muted-foreground">
                          {t("resultsDisplay.lessonPlanDuration")}
                        </span>
                        <span className="text-lg font-bold text-primary">
                          {durationRange[0]} - {durationRange[1]}
                        </span>
                      </div>
                      <Slider
                        min={normalizedDurationBounds[0]}
                        max={normalizedDurationBounds[1]}
                        step={1}
                        value={durationRange}
                        onValueChange={(value) =>
                          setDurationRange(value as [number, number])
                        }
                        className="w-full"
                      />
                    </div>

                    <div className="rounded-lg border border-border/50 bg-card/50 p-5 shadow-xs">
                      <div className="mb-1 flex items-center gap-2">
                        <Users className="h-4 w-4 text-primary" />
                        <Label className="font-semibold">{ t("resultsDisplay.activityNumber") }</Label>
                      </div>
                      <div className="mb-4 flex items-center justify-between">
                        <span className="text-sm text-muted-foreground">
                          {t("resultsDisplay.activityCountRange")}
                        </span>
                        <span className="text-lg font-bold text-primary">
                          {activityCountRange[0]} - {activityCountRange[1]}
                        </span>
                      </div>
                      <Slider
                        min={normalizedActivityCountBounds[0]}
                        max={normalizedActivityCountBounds[1]}
                        step={1}
                        value={activityCountRange}
                        onValueChange={(value) =>
                          setActivityCountRange(value as [number, number])
                        }
                        className="w-full"
                      />
                    </div>
                  </div>
                </div>
              )}

              <div className="rounded-xl border border-primary/10 bg-primary/5 p-6">
                <p className="font-medium text-primary">
                  {t("resultsDisplay.showingOf", {
                    filtered: filteredRecommendations.length,
                    total: results.activities.length,
                  })}
                </p>
              </div>
            </div>
          </CardContent>
        </Card>

        <div className="space-y-6">
          {filteredRecommendations.length > 0 ? (
            filteredRecommendations.map(({ recommendation }, index) => (
              <RecommendationRow
                key={`recommendation-${index}`}
                recommendation={recommendation}
                index={index}
                onSelect={() =>
                  handleCreateLessonPlanFromRecommendation(recommendation)
                }
              />
            ))
          ) : (
            <div className="rounded-xl border border-dashed border-border/70 bg-card/40 px-6 py-12 text-center">
              <AlertCircle className="mx-auto mb-4 h-10 w-10 text-muted-foreground" />
              <h3 className="mb-2 text-lg font-semibold text-foreground">
                {t("resultsDisplay.noMatchFilters")}
              </h3>
              <p className="text-sm text-muted-foreground">
                {t("resultsDisplay.noMatchFiltersDesc")}
              </p>
            </div>
          )}
        </div>
      </div>

      {/* Lesson Plan Dialog */}
      <Dialog open={isLessonPlanOpen} onOpenChange={handleCloseLessonPlan}>
        <DialogContent className="max-w-4xl max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>{t("resultsDisplay.lessonPlan")}</DialogTitle>
          </DialogHeader>
          {lessonPlanData && (
            <LessonPlanModal
              lessonPlanData={lessonPlanData}
              onClose={handleCloseLessonPlan}
            />
          )}
        </DialogContent>
      </Dialog>
    </>
  );
};
