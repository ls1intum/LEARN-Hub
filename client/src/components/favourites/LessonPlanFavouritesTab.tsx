import React, { useState, useEffect, useCallback, useMemo } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import {
  BookOpen,
  Trash2,
  Clock,
  Users,
  ChevronRight,
  Coffee,
  Layers,
} from "lucide-react";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Skeleton } from "@/components/ui/skeleton";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { PaginationBar } from "@/components/ui/PaginationBar";
import { ListFilterToolbar } from "@/components/lesson-plans/ListFilterToolbar";
import { RangeFilterSlider } from "@/components/lesson-plans/RangeFilterSlider";
import { ActivitySubRow } from "@/components/lesson-plans/ActivitySubRow";
import { apiService } from "@/services/apiService";
import { useAuth } from "@/hooks/useAuth";
import { useTranslateEnum } from "@/hooks/useTranslateEnum";
import type { Activity } from "@/types/activity";
import { useTranslation } from "react-i18next";
import { useRestoreScroll } from "@/hooks/useRestoreScroll";
import { getAppScrollTop } from "@/utils/scroll";

interface LessonPlanFavourite {
  id: string;
  favouriteType: string;
  name: string | null;
  activityIds: string[];
  lessonPlan?: import("@/types/activity").LessonPlanData;
  createdAt: string;
}

interface ResolvedPlan {
  favourite: LessonPlanFavourite;
  activities: Activity[];
  totalDuration: number;
  minAge: number;
  maxAge: number;
  activityCount: number;
  breakCount: number;
  formats: string[];
  searchText: string;
}

const ITEMS_PER_PAGE = 20;

export const LessonPlanFavouritesTab: React.FC = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const { t } = useTranslation();
  const translateEnum = useTranslateEnum();

  const [favourites, setFavourites] = useState<LessonPlanFavourite[]>([]);
  const [activitiesMap, setActivitiesMap] = useState<Map<string, Activity>>(
    new Map(),
  );
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [removingIds, setRemovingIds] = useState<Set<string>>(new Set());
  const [expandedIds, setExpandedIds] = useState<Set<string>>(new Set());
  const [search, setSearch] = useState("");
  const [currentPage, setCurrentPage] = useState(1);
  const [showFilters, setShowFilters] = useState(false);
  const [durationRange, setDurationRange] = useState<[number, number]>([
    0, 999,
  ]);
  const [activityCountRange, setActivityCountRange] = useState<
    [number, number]
  >([1, 99]);

  const loadFavourites = useCallback(async () => {
    if (!user) return;

    try {
      setLoading(true);
      setError(null);

      const response = await apiService.getLessonPlanFavourites();
      setFavourites(response.favourites);

      const idsNeedingFetch = response.favourites
        .filter((fav) => !fav.lessonPlan)
        .flatMap((fav) => fav.activityIds);
      const uniqueIds = [...new Set(idsNeedingFetch)];
      if (uniqueIds.length > 0) {
        const activityDetails = await apiService.getActivitiesByIds(uniqueIds);
        const next = new Map(activityDetails.map((a) => [a.id, a]));
        setActivitiesMap(next);
      }
    } catch (err) {
      setError(
        err instanceof Error ? err.message : "Failed to load favourites",
      );
    } finally {
      setLoading(false);
    }
  }, [user]);

  const removeFavourite = async (favouriteId: string) => {
    if (!user) return;

    try {
      setRemovingIds((prev) => new Set(prev).add(favouriteId));
      await apiService.deleteFavourite(favouriteId);
      setFavourites((prev) => prev.filter((fav) => fav.id !== favouriteId));
    } catch (err) {
      setError(
        err instanceof Error ? err.message : "Failed to remove favourite",
      );
    } finally {
      setRemovingIds((prev) => {
        const newSet = new Set(prev);
        newSet.delete(favouriteId);
        return newSet;
      });
    }
  };

  const toggleExpand = (id: string) => {
    setExpandedIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const handleViewActivityDetails = (activity: Activity) => {
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

  useEffect(() => {
    loadFavourites();
  }, [loadFavourites]);

  useRestoreScroll(!loading);

  // Resolve favourites into plan data
  const resolvedPlans: ResolvedPlan[] = useMemo(() => {
    return favourites
      .map((favourite) => {
        const activities: Activity[] = favourite.lessonPlan
          ? favourite.lessonPlan.activities
          : ((favourite.activityIds || [])
              .map((id) => activitiesMap.get(id))
              .filter(Boolean) as Activity[]);

        if (!activities || activities.length === 0) return null;

        const totalDuration = activities.reduce((sum, a) => {
          return (
            sum + (a.durationMinMinutes || 0) + (a.breakAfter?.duration || 0)
          );
        }, 0);
        const minAge = Math.min(...activities.map((a) => a.ageMin));
        const maxAge = Math.max(...activities.map((a) => a.ageMax));
        const breakCount = activities.filter((a) => a.breakAfter).length;
        const formats = [...new Set(activities.map((a) => a.format))];
        const searchText = [
          favourite.name || "",
          ...activities.flatMap((a) => [a.name, a.description]),
        ]
          .join(" ")
          .toLowerCase();

        return {
          favourite,
          activities,
          totalDuration,
          minAge,
          maxAge,
          activityCount: activities.length,
          breakCount,
          formats,
          searchText,
        };
      })
      .filter(Boolean) as ResolvedPlan[];
  }, [favourites, activitiesMap]);

  // Compute bounds for filters
  const durationBounds: [number, number] = useMemo(() => {
    if (resolvedPlans.length === 0) return [0, 0];
    const min = Math.min(...resolvedPlans.map((p) => p.totalDuration));
    const max = Math.max(...resolvedPlans.map((p) => p.totalDuration));
    return [min, max];
  }, [resolvedPlans]);

  const activityCountBounds: [number, number] = useMemo(() => {
    if (resolvedPlans.length === 0) return [1, 1];
    const min = Math.min(...resolvedPlans.map((p) => p.activityCount));
    const max = Math.max(...resolvedPlans.map((p) => p.activityCount));
    return [min, max];
  }, [resolvedPlans]);

  // Reset filter ranges when data loads
  useEffect(() => {
    setDurationRange(durationBounds);
    setActivityCountRange(activityCountBounds);
  }, [durationBounds, activityCountBounds]);

  // Client-side filtering
  const filteredPlans = useMemo(() => {
    const q = search.trim().toLowerCase();
    return resolvedPlans.filter((plan) => {
      if (q && !plan.searchText.includes(q)) return false;
      if (
        plan.totalDuration < durationRange[0] ||
        plan.totalDuration > durationRange[1]
      )
        return false;
      if (
        plan.activityCount < activityCountRange[0] ||
        plan.activityCount > activityCountRange[1]
      )
        return false;
      return true;
    });
  }, [resolvedPlans, search, durationRange, activityCountRange]);

  useEffect(() => {
    setCurrentPage(1);
  }, [search, durationRange, activityCountRange]);

  const totalPages = Math.ceil(filteredPlans.length / ITEMS_PER_PAGE);
  const pagedPlans = filteredPlans.slice(
    (currentPage - 1) * ITEMS_PER_PAGE,
    currentPage * ITEMS_PER_PAGE,
  );

  const activeFilterCount = useMemo(() => {
    let count = 0;
    if (search) count++;
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
    search,
    durationRange,
    durationBounds,
    activityCountRange,
    activityCountBounds,
  ]);

  const clearFilters = () => {
    setSearch("");
    setDurationRange(durationBounds);
    setActivityCountRange(activityCountBounds);
    setShowFilters(false);
  };

  if (loading) {
    return (
      <div className="border border-border rounded-lg overflow-hidden">
        <div className="flex items-center gap-3 px-3 py-2 bg-muted/40 border-b border-border">
          <Skeleton className="h-4 w-32" />
        </div>
        <div className="divide-y divide-border">
          {[...Array(4)].map((_, i) => (
            <div key={i} className="flex items-center gap-3 px-3 py-2.5">
              <Skeleton className="h-4 flex-1" />
              <Skeleton className="h-5 w-14 hidden sm:block" />
              <Skeleton className="h-5 w-14 hidden sm:block" />
              <Skeleton className="h-5 w-16" />
              <Skeleton className="h-7 w-[56px]" />
            </div>
          ))}
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <Alert variant="destructive">
        <AlertDescription>{error}</AlertDescription>
      </Alert>
    );
  }

  if (favourites.length === 0) {
    return (
      <div className="text-center py-12">
        <BookOpen className="mx-auto h-12 w-12 text-muted-foreground mb-4" />
        <h3 className="text-lg font-semibold text-foreground mb-2">
          {t("lessonPlanFavourites.noFavourites")}
        </h3>
        <p className="text-muted-foreground">
          {t("lessonPlanFavourites.noFavouritesDesc")}
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <ListFilterToolbar
        searchValue={search}
        onSearchChange={setSearch}
        searchPlaceholder={t("lessonPlanFavourites.searchPlaceholder")}
        showFilters={showFilters}
        onToggleFilters={() => setShowFilters(!showFilters)}
        activeFilterCount={activeFilterCount}
        onClearFilters={clearFilters}
      />

      {showFilters && (
        <div className="border border-border rounded-lg p-4 space-y-5 bg-muted/20">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
            <RangeFilterSlider
              icon={Clock}
              label={t("lessonPlanFavourites.totalDuration")}
              value={durationRange}
              onChange={(v) => setDurationRange(v as [number, number])}
              min={durationBounds[0]}
              max={durationBounds[1]}
              formatValue={(v) => `${v[0]}–${v[1]}m`}
            />
            <RangeFilterSlider
              icon={Layers}
              label={t("lessonPlanFavourites.activityCount")}
              value={activityCountRange}
              onChange={(v) => setActivityCountRange(v as [number, number])}
              min={activityCountBounds[0]}
              max={activityCountBounds[1]}
              formatValue={(v) => `${v[0]}–${v[1]}`}
            />
          </div>
        </div>
      )}

      <p className="text-xs text-muted-foreground tabular-nums">
        {filteredPlans.length} {t("lessonPlanFavourites.totalPlans")}
      </p>

      {/* Table */}
      <div className="border border-border rounded-lg overflow-hidden">
        {/* Column headers */}
        <div className="flex items-center gap-2 px-3 py-2 bg-muted/30 border-b border-border">
          <span className="w-6 shrink-0" />
          <span className="flex-1 text-xs font-medium text-muted-foreground">
            {t("activityFavourites.nameHeader")}
          </span>
          <span className="w-[68px] text-xs font-medium text-muted-foreground hidden sm:block shrink-0">
            {t("lessonPlanFavourites.colActivities")}
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
          <span className="w-[100px] text-xs font-medium text-muted-foreground hidden lg:block shrink-0">
            {t("activityFavourites.dateHeader")}
          </span>
          <span className="w-[32px] shrink-0" />
        </div>

        {pagedPlans.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-10 text-center">
            <p className="text-sm text-muted-foreground">
              {t("lessonPlanFavourites.noResults")}
            </p>
          </div>
        ) : (
          <div className="divide-y divide-border">
            {pagedPlans.map((plan) => {
              const isExpanded = expandedIds.has(plan.favourite.id);
              const isRemoving = removingIds.has(plan.favourite.id);

              return (
                <React.Fragment key={plan.favourite.id}>
                  {/* Summary row */}
                  <div
                    className="flex items-center gap-2 px-3 py-2.5 hover:bg-muted/30 transition-colors cursor-pointer"
                    onClick={() => toggleExpand(plan.favourite.id)}
                  >
                    <div className="w-6 shrink-0 flex items-center justify-center">
                      <ChevronRight
                        className={`h-3.5 w-3.5 text-muted-foreground transition-transform duration-200 ${
                          isExpanded ? "rotate-90" : ""
                        }`}
                      />
                    </div>

                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-medium truncate">
                        {plan.favourite.name || t("lessonPlan.untitled")}
                      </p>
                    </div>

                    <div className="w-[68px] shrink-0 hidden sm:flex items-center gap-1 text-xs text-muted-foreground">
                      <Layers className="h-3 w-3 shrink-0" />
                      <span className="tabular-nums">{plan.activityCount}</span>
                      {plan.breakCount > 0 && (
                        <span className="flex items-center gap-0.5 text-blue-500">
                          <Coffee className="h-2.5 w-2.5" />
                          {plan.breakCount}
                        </span>
                      )}
                    </div>

                    <div className="w-[68px] shrink-0 hidden sm:flex items-center gap-0.5 text-xs text-muted-foreground">
                      <Clock className="h-3 w-3 shrink-0" />
                      <span className="tabular-nums">
                        {plan.totalDuration}m
                      </span>
                    </div>

                    <div className="w-[52px] shrink-0 hidden md:flex items-center gap-0.5 text-xs text-muted-foreground">
                      <Users className="h-3 w-3 shrink-0" />
                      {plan.minAge}–{plan.maxAge}
                    </div>

                    <div className="w-[76px] shrink-0">
                      <Badge
                        variant="secondary"
                        className="text-[11px] px-1.5 py-0 font-normal truncate max-w-full"
                      >
                        {translateEnum("format", plan.formats[0])}
                      </Badge>
                    </div>

                    <div className="w-[100px] shrink-0 hidden lg:block text-xs text-muted-foreground tabular-nums">
                      {new Date(plan.favourite.createdAt).toLocaleDateString()}
                    </div>

                    <div className="w-[32px] shrink-0 flex justify-end">
                      <Button
                        variant="ghost"
                        size="icon"
                        className="h-7 w-7 text-destructive"
                        onClick={(e) => {
                          e.stopPropagation();
                          removeFavourite(plan.favourite.id);
                        }}
                        disabled={isRemoving}
                        aria-label="Remove favourite"
                      >
                        <Trash2 className="h-3.5 w-3.5" />
                      </Button>
                    </div>
                  </div>

                  {/* Expanded activity sub-rows */}
                  {isExpanded && (
                    <div className="bg-muted/10 border-t border-border/50">
                      {plan.activities.map((activity, idx) => (
                        <ActivitySubRow
                          key={`${plan.favourite.id}-${activity.id || idx}`}
                          activity={activity}
                          index={idx}
                          onClick={handleViewActivityDetails}
                        />
                      ))}
                    </div>
                  )}
                </React.Fragment>
              );
            })}
          </div>
        )}

        {pagedPlans.length > 0 && (
          <div className="px-3">
            <PaginationBar
              currentPage={currentPage}
              totalPages={totalPages}
              totalItems={filteredPlans.length}
              itemsPerPage={ITEMS_PER_PAGE}
              onPageChange={setCurrentPage}
            />
          </div>
        )}
      </div>
    </div>
  );
};
