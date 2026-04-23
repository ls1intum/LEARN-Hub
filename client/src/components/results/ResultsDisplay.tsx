import React, { useEffect, useState, useMemo } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { LessonPlanModal } from "@/components/LessonPlanModal";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { LessonPlanFavouriteButton } from "@/components/favourites/LessonPlanFavouriteButton";
import { ListFilterToolbar } from "@/components/lesson-plans/ListFilterToolbar";
import { RangeFilterSlider } from "@/components/lesson-plans/RangeFilterSlider";
import { ActivitySubRow } from "@/components/lesson-plans/ActivitySubRow";
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import {
  AlertCircle,
  ChevronRight,
  Clock,
  Coffee,
  Layers,
  Sparkles,
  Target,
  Users,
} from "lucide-react";
import type {
  ResultsData,
  Recommendation,
  LessonPlanData,
} from "@/types/activity";
import { RecommendationCard } from "@/components/results/RecommendationCard";
import { ViewToggle, type ViewMode } from "@/components/ui/ViewToggle";
import { useTranslation } from "react-i18next";
import { useTranslateEnum } from "@/hooks/useTranslateEnum";
import { getAppScrollTop } from "@/utils/scroll";

interface ResultsDisplayProps {
  results: ResultsData;
  className?: string;
}

interface ResolvedRecommendation {
  recommendation: Recommendation;
  totalDuration: number;
  activityCount: number;
  breakCount: number;
  minAge: number;
  maxAge: number;
  formats: string[];
  searchText: string;
}

export const ResultsDisplay: React.FC<ResultsDisplayProps> = ({
  results,
  className = "",
}) => {
  const { t } = useTranslation();
  const translateEnum = useTranslateEnum();
  const navigate = useNavigate();
  const location = useLocation();
  const hasRecommendations = Boolean(results?.activities?.length);
  const [isLessonPlanOpen, setIsLessonPlanOpen] = useState(false);
  const [lessonPlanData, setLessonPlanData] = useState<LessonPlanData | null>(
    null,
  );
  const [expandedIds, setExpandedIds] = useState<Set<number>>(new Set());
  const [viewMode, setViewMode] = useState<ViewMode>(
    () => (localStorage.getItem("view-recommendations") as ViewMode) ?? "grid",
  );
  const handleViewChange = (mode: ViewMode) => {
    setViewMode(mode);
    localStorage.setItem("view-recommendations", mode);
  };

  // Filters
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
    const totalDuration = recommendation.activities.reduce(
      (total, activity) => total + (activity.durationMinMinutes || 0),
      0,
    );

    const data: LessonPlanData = {
      activities: recommendation.activities,
      totalDurationMinutes: totalDuration,
      breaks: [],
      ordering_strategy: "balanced",
      title: t("lessonPlanFavourite.defaultTitle"),
    };

    setLessonPlanData(data);
    setIsLessonPlanOpen(true);
  };

  const handleCloseLessonPlan = () => {
    setIsLessonPlanOpen(false);
    setLessonPlanData(null);
  };

  const handleViewActivityDetails = (
    activity: Recommendation["activities"][0],
  ) => {
    if (activity.id && activity.type === "activity") {
      navigate(`/activity-details/${activity.id}`, {
        state: {
          activity,
          backTo: `${location.pathname}${location.search}`,
          restoreScrollY: getAppScrollTop(),
        },
      });
    }
  };

  const toggleExpand = (index: number) => {
    setExpandedIds((prev) => {
      const next = new Set(prev);
      if (next.has(index)) next.delete(index);
      else next.add(index);
      return next;
    });
  };

  // Resolve recommendations
  const resolved: ResolvedRecommendation[] = useMemo(
    () =>
      (results?.activities ?? []).map((recommendation) => {
        const { activities } = recommendation;
        const totalDuration = activities.reduce(
          (sum, a) =>
            sum + (a.durationMinMinutes || 0) + (a.breakAfter?.duration || 0),
          0,
        );
        const breakCount = activities.filter((a) => a.breakAfter).length;
        const minAge =
          activities.length > 0
            ? Math.min(...activities.map((a) => a.ageMin))
            : 0;
        const maxAge =
          activities.length > 0
            ? Math.max(...activities.map((a) => a.ageMax))
            : 0;
        const formats = [...new Set(activities.map((a) => a.format))];
        const searchText = activities
          .flatMap((a) => [a.name, a.description])
          .join(" ")
          .toLowerCase();

        return {
          recommendation,
          totalDuration,
          activityCount: activities.length,
          breakCount,
          minAge,
          maxAge,
          formats,
          searchText,
        };
      }),
    [results],
  );

  // Compute bounds
  const durationBounds: [number, number] = useMemo(() => {
    if (resolved.length === 0) return [0, 0];
    const min = Math.min(...resolved.map((r) => r.totalDuration));
    const max = Math.max(...resolved.map((r) => r.totalDuration));
    return [min, max];
  }, [resolved]);

  const activityCountBounds: [number, number] = useMemo(() => {
    if (resolved.length === 0) return [1, 1];
    const min = Math.min(...resolved.map((r) => r.activityCount));
    const max = Math.max(...resolved.map((r) => r.activityCount));
    return [min, max];
  }, [resolved]);

  // Reset filters on new results
  useEffect(() => {
    setSearchQuery("");
    setScoreThreshold(0);
    setDurationRange(durationBounds);
    setActivityCountRange(activityCountBounds);
    setShowFilters(false);
    setExpandedIds(new Set());
  }, [results, durationBounds, activityCountBounds]);

  const filtered = useMemo(() => {
    const q = searchQuery.trim().toLowerCase();
    return resolved.filter((item) => {
      if (q && !item.searchText.includes(q)) return false;
      if (item.recommendation.score < scoreThreshold) return false;
      if (
        item.totalDuration < durationRange[0] ||
        item.totalDuration > durationRange[1]
      )
        return false;
      if (
        item.activityCount < activityCountRange[0] ||
        item.activityCount > activityCountRange[1]
      )
        return false;
      return true;
    });
  }, [
    resolved,
    searchQuery,
    scoreThreshold,
    durationRange,
    activityCountRange,
  ]);

  const activeFilterCount = useMemo(() => {
    let count = 0;
    if (searchQuery) count++;
    if (scoreThreshold > 0) count++;
    if (
      durationRange[0] !== durationBounds[0] ||
      durationRange[1] !== durationBounds[1]
    )
      count++;
    if (
      activityCountRange[0] !== activityCountBounds[0] ||
      activityCountRange[1] !== activityCountBounds[1]
    )
      count++;
    return count;
  }, [
    searchQuery,
    scoreThreshold,
    durationRange,
    durationBounds,
    activityCountRange,
    activityCountBounds,
  ]);

  const resetFilters = () => {
    setSearchQuery("");
    setScoreThreshold(0);
    setDurationRange(durationBounds);
    setActivityCountRange(activityCountBounds);
    setShowFilters(false);
  };

  const getScoreColor = (score: number) => {
    if (score >= 80) return "bg-green-500";
    if (score >= 60) return "bg-yellow-500";
    if (score >= 40) return "bg-orange-500";
    return "bg-red-500";
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
      <div className={`space-y-4 ${className}`}>
        <ListFilterToolbar
          searchValue={searchQuery}
          onSearchChange={setSearchQuery}
          searchPlaceholder={t("resultsDisplay.searchPlaceholder")}
          showFilters={showFilters}
          onToggleFilters={() => setShowFilters(!showFilters)}
          activeFilterCount={activeFilterCount}
          onClearFilters={resetFilters}
        />

        {showFilters && (
          <div className="border border-border rounded-lg p-4 space-y-5 bg-muted/20">
            <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
              <RangeFilterSlider
                icon={Target}
                label={t("resultsDisplay.matchPercentage")}
                value={[scoreThreshold]}
                onChange={([value]) => setScoreThreshold(value ?? 0)}
                min={0}
                max={100}
                formatValue={(v) => `${v[0]}%+`}
              />
              <RangeFilterSlider
                icon={Clock}
                label={t("resultsDisplay.length")}
                value={durationRange}
                onChange={(v) => setDurationRange(v as [number, number])}
                min={durationBounds[0]}
                max={durationBounds[1]}
                formatValue={(v) => `${v[0]}–${v[1]}m`}
              />
              <RangeFilterSlider
                icon={Layers}
                label={t("resultsDisplay.activityNumber")}
                value={activityCountRange}
                onChange={(v) => setActivityCountRange(v as [number, number])}
                min={activityCountBounds[0]}
                max={activityCountBounds[1]}
                formatValue={(v) => `${v[0]}–${v[1]}`}
              />
            </div>
          </div>
        )}

        <div className="flex items-center justify-between">
          <p className="text-xs text-muted-foreground tabular-nums">
            {t("resultsDisplay.showingOf", {
              filtered: filtered.length,
              total: results.activities.length,
            })}
          </p>
          <ViewToggle value={viewMode} onChange={handleViewChange} />
        </div>

        {filtered.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-10 text-center border border-border rounded-lg">
            <AlertCircle className="mx-auto mb-3 h-8 w-8 text-muted-foreground" />
            <p className="text-sm font-medium mb-1">
              {t("resultsDisplay.noMatchFilters")}
            </p>
            <p className="text-xs text-muted-foreground">
              {t("resultsDisplay.noMatchFiltersDesc")}
            </p>
          </div>
        ) : viewMode === "grid" ? (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            {filtered.map(
              (
                {
                  recommendation,
                  totalDuration,
                  activityCount,
                  breakCount,
                  minAge,
                  maxAge,
                  formats,
                },
                index,
              ) => (
                <RecommendationCard
                  key={`rec-${index}`}
                  recommendation={recommendation}
                  totalDuration={totalDuration}
                  activityCount={activityCount}
                  breakCount={breakCount}
                  minAge={minAge}
                  maxAge={maxAge}
                  formats={formats}
                  onSelect={() =>
                    handleCreateLessonPlanFromRecommendation(recommendation)
                  }
                  onViewActivity={handleViewActivityDetails}
                />
              ),
            )}
          </div>
        ) : (
          /* Table view */
          <div className="border border-border rounded-lg overflow-hidden">
            {/* Column headers */}
            <div className="flex items-center gap-2 px-3 py-2 bg-muted/30 border-b border-border">
              <span className="w-6 shrink-0" />
              <span className="w-[52px] text-xs font-medium text-muted-foreground shrink-0">
                {t("resultsDisplay.colScore")}
              </span>
              <span className="flex-1 text-xs font-medium text-muted-foreground">
                {t("resultsDisplay.colActivities")}
              </span>
              <span className="w-[68px] text-xs font-medium text-muted-foreground hidden sm:block shrink-0">
                {t("activityFavourites.durationHeader")}
              </span>
              <span className="w-[52px] text-xs font-medium text-muted-foreground hidden md:block shrink-0">
                {t("activityFavourites.ageHeader")}
              </span>
              <span className="w-[76px] text-xs font-medium text-muted-foreground shrink-0">
                {t("activityFavourites.formatHeader")}
              </span>
              <span className="w-[140px] shrink-0" />
            </div>

            <div className="divide-y divide-border">
              {filtered.map(
                (
                  {
                    recommendation,
                    totalDuration,
                    activityCount,
                    breakCount,
                    minAge,
                    maxAge,
                    formats,
                  },
                  index,
                ) => {
                  const isExpanded = expandedIds.has(index);
                  const { score, scoreBreakdown, activities } = recommendation;

                  return (
                    <React.Fragment key={`rec-${index}`}>
                      {/* Summary row */}
                      <div
                        className="flex items-center gap-2 px-3 py-2.5 hover:bg-muted/30 transition-colors cursor-pointer"
                        onClick={() => toggleExpand(index)}
                      >
                        <div className="w-6 shrink-0 flex items-center justify-center">
                          <ChevronRight
                            className={`h-3.5 w-3.5 text-muted-foreground transition-transform duration-200 ${
                              isExpanded ? "rotate-90" : ""
                            }`}
                          />
                        </div>

                        {/* Score */}
                        <div className="w-[52px] shrink-0">
                          <TooltipProvider>
                            <Tooltip>
                              <TooltipTrigger asChild>
                                <div
                                  className="flex items-center gap-1.5 cursor-pointer"
                                  onClick={(e) => e.stopPropagation()}
                                >
                                  <div
                                    className={`w-2 h-2 rounded-full shrink-0 ${getScoreColor(score)}`}
                                  />
                                  <span className="text-sm font-semibold tabular-nums">
                                    {Math.round(score)}%
                                  </span>
                                </div>
                              </TooltipTrigger>
                              <TooltipContent
                                side="bottom"
                                className="max-w-xs"
                              >
                                <div className="space-y-2">
                                  <div className="font-semibold text-xs">
                                    {t("resultsDisplay.categoryScores")}
                                  </div>
                                  <div className="space-y-1 text-xs">
                                    {Object.entries(scoreBreakdown || {}).map(
                                      ([category, scoreData]) => (
                                        <div
                                          key={category}
                                          className="flex justify-between gap-4"
                                        >
                                          <span className="capitalize">
                                            {category.replace(/_/g, " ")}
                                          </span>
                                          <span className="font-medium tabular-nums">
                                            {scoreData.score}%
                                            {scoreData.isPriority && (
                                              <span className="ml-1 text-yellow-500">
                                                ★
                                              </span>
                                            )}
                                          </span>
                                        </div>
                                      ),
                                    )}
                                  </div>
                                </div>
                              </TooltipContent>
                            </Tooltip>
                          </TooltipProvider>
                        </div>

                        {/* Activities summary */}
                        <div className="flex-1 min-w-0">
                          <div className="flex items-center gap-2 text-sm">
                            <span className="font-medium truncate">
                              {activities.map((a) => a.name).join(", ")}
                            </span>
                          </div>
                          <div className="flex items-center gap-2 text-xs text-muted-foreground mt-0.5">
                            <span className="flex items-center gap-0.5">
                              <Layers className="h-3 w-3" />
                              {activityCount}
                            </span>
                            {breakCount > 0 && (
                              <span className="flex items-center gap-0.5 text-blue-500">
                                <Coffee className="h-2.5 w-2.5" />
                                {breakCount}
                              </span>
                            )}
                          </div>
                        </div>

                        <div className="w-[68px] shrink-0 hidden sm:flex items-center gap-0.5 text-xs text-muted-foreground">
                          <Clock className="h-3 w-3 shrink-0" />
                          <span className="tabular-nums">{totalDuration}m</span>
                        </div>

                        <div className="w-[52px] shrink-0 hidden md:flex items-center gap-0.5 text-xs text-muted-foreground">
                          <Users className="h-3 w-3 shrink-0" />
                          {minAge}–{maxAge}
                        </div>

                        <div className="w-[76px] shrink-0">
                          <Badge
                            variant="secondary"
                            className="text-[11px] px-1.5 py-0 font-normal truncate max-w-full"
                          >
                            {translateEnum("format", formats[0])}
                          </Badge>
                        </div>

                        {/* Actions */}
                        <div
                          className="w-[140px] shrink-0 flex items-center justify-end gap-1.5"
                          onClick={(e) => e.stopPropagation()}
                          onPointerDown={(e) => e.stopPropagation()}
                        >
                          <LessonPlanFavouriteButton
                            activities={activities}
                            size="icon"
                            className="h-7 w-7 shrink-0"
                          />
                          <Button
                            size="sm"
                            onClick={() =>
                              handleCreateLessonPlanFromRecommendation(
                                recommendation,
                              )
                            }
                            className="h-7 px-2 text-xs"
                          >
                            <Sparkles className="h-3 w-3 mr-1" />
                            {t("resultsDisplay.select")}
                          </Button>
                        </div>
                      </div>

                      {/* Expanded activity sub-rows */}
                      {isExpanded && (
                        <div className="bg-muted/10 border-t border-border/50">
                          {activities.map((activity, idx) => (
                            <ActivitySubRow
                              key={`${index}-${activity.id || idx}`}
                              activity={activity}
                              index={idx}
                              onClick={handleViewActivityDetails}
                              showDescription
                            />
                          ))}
                        </div>
                      )}
                    </React.Fragment>
                  );
                },
              )}
            </div>
          </div>
        )}
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
