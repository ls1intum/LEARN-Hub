import React, { useState, useCallback, useEffect, useMemo } from "react";
import { useLocation, useNavigate, useSearchParams } from "react-router-dom";
import {
  Heart,
  Trash2,
  Clock,
  Users,
  Grid3x3,
  GraduationCap,
  Package,
  Tag,
  Brain,
  Activity as ActivityIcon,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Slider } from "@/components/ui/slider";
import { BadgeSelector } from "@/components/ui/BadgeSelector";
import { ActivityCard, ActivityCardSkeleton } from "@/components/ActivityCard";
import { ViewToggle, type ViewMode } from "@/components/ui/ViewToggle";
import { PaginationBar } from "@/components/ui/PaginationBar";
import { ListFilterToolbar } from "@/components/lesson-plans/ListFilterToolbar";
import { apiService } from "@/services/apiService";
import { useAuth } from "@/hooks/useAuth";
import { useDataFetch } from "@/hooks/useDataFetch";
import { useDebouncedValue } from "@/hooks/useDebouncedValue";
import { useFieldValues } from "@/hooks/useFieldValues";
import { ACTIVITY_CONSTANTS } from "@/constants/activity";
import { useTranslation } from "react-i18next";
import { useTranslateEnum } from "@/hooks/useTranslateEnum";
import { useRestoreScroll } from "@/hooks/useRestoreScroll";
import { getAppScrollTop } from "@/utils/scroll";
import type { Activity, FilterOptions } from "@/types/activity";

const ITEMS_PER_PAGE = 20;

interface FilterFormData {
  name: string;
  ageMin: number;
  ageMax: number;
  format: string[];
  bloomLevel: string[];
  resourcesNeeded: string[];
  topics: string[];
  mentalLoad: string[];
  physicalEnergy: string[];
  durationMin: number;
  durationMax: number;
}

const initialFilterData: FilterFormData = {
  name: "",
  ageMin: ACTIVITY_CONSTANTS.AGE_RANGE.MIN,
  ageMax: ACTIVITY_CONSTANTS.AGE_RANGE.MAX,
  format: [],
  bloomLevel: [],
  resourcesNeeded: [],
  topics: [],
  mentalLoad: [],
  physicalEnergy: [],
  durationMin: ACTIVITY_CONSTANTS.DURATION_RANGE.MIN,
  durationMax: ACTIVITY_CONSTANTS.DURATION_RANGE.MAX,
};

export const ActivityFavouritesTab: React.FC = () => {
  const { user } = useAuth();
  const { fieldValues } = useFieldValues();
  const navigate = useNavigate();
  const location = useLocation();
  const [searchParams, setSearchParams] = useSearchParams();
  const { t } = useTranslation();
  const translateEnum = useTranslateEnum();

  const [currentPage, setCurrentPage] = useState(() => {
    const p = Number(searchParams.get("actPage"));
    return p > 0 ? p : 1;
  });
  const [filters, setFilters] = useState<FilterFormData>(() => ({
    ...initialFilterData,
    name: searchParams.get("actSearch") ?? "",
  }));
  const [showFilters, setShowFilters] = useState(false);
  const debouncedSearch = useDebouncedValue(filters.name, 300);
  const [removingIds, setRemovingIds] = useState<Set<string>>(new Set());
  const [viewMode, setViewMode] = useState<ViewMode>(
    () =>
      (localStorage.getItem("view-activity-favourites") as ViewMode) ?? "grid",
  );

  const filterOptions: FilterOptions = useMemo(
    () => ({
      format: fieldValues.format,
      resourcesAvailable: fieldValues.resourcesAvailable,
      bloomLevel: fieldValues.bloomLevel,
      topics: fieldValues.topics,
      mentalLoad: fieldValues.mentalLoad,
      physicalEnergy: fieldValues.physicalEnergy,
    }),
    [fieldValues],
  );

  const handleViewChange = (mode: ViewMode) => {
    setViewMode(mode);
    localStorage.setItem("view-activity-favourites", mode);
  };

  const updateParams = useCallback(
    (nextPage: number, nextSearch: string) => {
      const params = new URLSearchParams(searchParams);
      if (nextSearch) params.set("actSearch", nextSearch);
      else params.delete("actSearch");
      if (nextPage > 1) params.set("actPage", String(nextPage));
      else params.delete("actPage");
      if (params.toString() !== searchParams.toString())
        setSearchParams(params, { replace: true });
    },
    [searchParams, setSearchParams],
  );

  const updateFilters = useCallback(
    (nextFilters: FilterFormData) => {
      setFilters(nextFilters);
      setCurrentPage(1);
      updateParams(1, nextFilters.name);
    },
    [updateParams],
  );

  const handleSearchChange = (value: string) => {
    updateFilters({ ...filters, name: value });
  };

  const handleMultiSelectFilter = (
    filterType: keyof FilterFormData,
    value: string,
    checked: boolean,
  ) => {
    const currentValues = filters[filterType] as string[];
    const nextValues = checked
      ? [...currentValues, value]
      : currentValues.filter((item) => item !== value);
    updateFilters({
      ...filters,
      [filterType]: nextValues,
    } as FilterFormData);
  };

  const handleRangeFilterChange = (
    minField: "ageMin" | "durationMin",
    maxField: "ageMax" | "durationMax",
    values: number[],
  ) => {
    updateFilters({
      ...filters,
      [minField]: values[0],
      [maxField]: values[1],
    } as FilterFormData);
  };

  const handlePageChange = (page: number) => {
    setCurrentPage(page);
    updateParams(page, filters.name);
  };

  const clearFilters = () => {
    updateFilters(initialFilterData);
    setShowFilters(false);
  };

  const fetchFavourites = useCallback(async () => {
    return await apiService.getActivityFavourites({
      limit: ITEMS_PER_PAGE,
      offset: (currentPage - 1) * ITEMS_PER_PAGE,
      name: debouncedSearch || undefined,
      ageMin:
        filters.ageMin > ACTIVITY_CONSTANTS.AGE_RANGE.MIN
          ? filters.ageMin
          : undefined,
      ageMax:
        filters.ageMax < ACTIVITY_CONSTANTS.AGE_RANGE.MAX
          ? filters.ageMax
          : undefined,
      durationMin:
        filters.durationMin > ACTIVITY_CONSTANTS.DURATION_RANGE.MIN
          ? filters.durationMin
          : undefined,
      durationMax:
        filters.durationMax < ACTIVITY_CONSTANTS.DURATION_RANGE.MAX
          ? filters.durationMax
          : undefined,
      format: filters.format.length > 0 ? filters.format : undefined,
      bloomLevel:
        filters.bloomLevel.length > 0 ? filters.bloomLevel : undefined,
      resourcesNeeded:
        filters.resourcesNeeded.length > 0
          ? filters.resourcesNeeded
          : undefined,
      topics: filters.topics.length > 0 ? filters.topics : undefined,
      mentalLoad:
        filters.mentalLoad.length > 0 ? filters.mentalLoad : undefined,
      physicalEnergy:
        filters.physicalEnergy.length > 0 ? filters.physicalEnergy : undefined,
    });
  }, [currentPage, debouncedSearch, filters]);

  const { data, isLoading, error, refetch } = useDataFetch({
    fetchFn: fetchFavourites,
    enabled: !!user,
  });

  const removeFavourite = async (activityId: string) => {
    if (!user) return;
    try {
      setRemovingIds((prev) => new Set(prev).add(activityId));
      await apiService.removeActivityFavourite(activityId);
      await refetch();
    } finally {
      setRemovingIds((prev) => {
        const next = new Set(prev);
        next.delete(activityId);
        return next;
      });
    }
  };

  const handleViewDetails = (activityId: string, activity: Activity) => {
    navigate(`/favourites/${activityId}`, {
      state: {
        activity,
        backTo: `${location.pathname}${location.search}`,
        restoreScrollY: getAppScrollTop(),
      },
    });
  };

  const activeFilterCount = useMemo(() => {
    let count = 0;
    if (filters.name) count++;
    if (
      filters.ageMin !== initialFilterData.ageMin ||
      filters.ageMax !== initialFilterData.ageMax
    )
      count++;
    if (
      filters.durationMin !== initialFilterData.durationMin ||
      filters.durationMax !== initialFilterData.durationMax
    )
      count++;
    if (filters.format.length > 0) count++;
    if (filters.bloomLevel.length > 0) count++;
    if (filters.resourcesNeeded.length > 0) count++;
    if (filters.topics.length > 0) count++;
    if (filters.mentalLoad.length > 0) count++;
    if (filters.physicalEnergy.length > 0) count++;
    return count;
  }, [filters]);

  const favourites = data?.favourites ?? [];
  const total = data?.total ?? 0;
  const totalPages = Math.ceil(total / ITEMS_PER_PAGE);

  useEffect(() => {
    if (currentPage > totalPages && totalPages > 0) {
      setCurrentPage(totalPages);
      updateParams(totalPages, filters.name);
    }
  }, [currentPage, filters.name, totalPages, updateParams]);

  useRestoreScroll(!isLoading);

  const showEmptyState = !isLoading && total === 0 && activeFilterCount === 0;

  return (
    <div className="space-y-4">
      <ListFilterToolbar
        searchValue={filters.name}
        onSearchChange={handleSearchChange}
        searchPlaceholder={t("activityFavourites.searchPlaceholder")}
        showFilters={showFilters}
        onToggleFilters={() => setShowFilters((prev) => !prev)}
        activeFilterCount={activeFilterCount}
        onClearFilters={clearFilters}
      />

      {showFilters && (
        <div className="border border-border rounded-lg p-4 space-y-5 bg-muted/20">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-1.5">
                  <Users className="h-3.5 w-3.5 text-muted-foreground" />
                  <span className="text-sm font-medium">
                    {t("library.ageRange")}
                  </span>
                </div>
                <span className="text-sm font-semibold text-primary tabular-nums">
                  {filters.ageMin}–{filters.ageMax}
                </span>
              </div>
              <Slider
                value={[filters.ageMin, filters.ageMax]}
                onValueChange={(value) =>
                  handleRangeFilterChange("ageMin", "ageMax", value)
                }
                max={ACTIVITY_CONSTANTS.AGE_RANGE.MAX}
                min={ACTIVITY_CONSTANTS.AGE_RANGE.MIN}
                step={1}
              />
            </div>
            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-1.5">
                  <Clock className="h-3.5 w-3.5 text-muted-foreground" />
                  <span className="text-sm font-medium">
                    {t("library.durationRange")}
                  </span>
                </div>
                <span className="text-sm font-semibold text-primary tabular-nums">
                  {filters.durationMin}–{filters.durationMax}m
                </span>
              </div>
              <Slider
                value={[filters.durationMin, filters.durationMax]}
                onValueChange={(value) =>
                  handleRangeFilterChange("durationMin", "durationMax", value)
                }
                max={ACTIVITY_CONSTANTS.DURATION_RANGE.MAX}
                min={ACTIVITY_CONSTANTS.DURATION_RANGE.MIN}
                step={ACTIVITY_CONSTANTS.DURATION_RANGE.STEP}
              />
            </div>
          </div>

          <div className="border-t border-border" />

          <div className="space-y-2">
            <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wide">
              {t("library.activityCharacteristics")}
            </p>
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
              <div className="space-y-1.5">
                <div className="flex items-center gap-1.5">
                  <Grid3x3 className="h-3.5 w-3.5 text-muted-foreground" />
                  <span className="text-xs font-medium text-muted-foreground">
                    {t("library.format")}
                  </span>
                </div>
                <BadgeSelector
                  label=""
                  options={filterOptions.format}
                  selectedValues={filters.format}
                  allowEmptySelection
                  onToggle={(value) =>
                    handleMultiSelectFilter(
                      "format",
                      value,
                      !filters.format.includes(value),
                    )
                  }
                  labelFn={(value) => translateEnum("format", value)}
                />
              </div>
              <div className="space-y-1.5">
                <div className="flex items-center gap-1.5">
                  <GraduationCap className="h-3.5 w-3.5 text-muted-foreground" />
                  <span className="text-xs font-medium text-muted-foreground">
                    {t("library.bloomLevel")}
                  </span>
                </div>
                <BadgeSelector
                  label=""
                  options={filterOptions.bloomLevel}
                  selectedValues={filters.bloomLevel}
                  allowEmptySelection
                  onToggle={(value) =>
                    handleMultiSelectFilter(
                      "bloomLevel",
                      value,
                      !filters.bloomLevel.includes(value),
                    )
                  }
                  labelFn={(value) => translateEnum("bloomLevel", value)}
                />
              </div>
              <div className="space-y-1.5">
                <div className="flex items-center gap-1.5">
                  <Package className="h-3.5 w-3.5 text-muted-foreground" />
                  <span className="text-xs font-medium text-muted-foreground">
                    {t("library.resourcesNeeded")}
                  </span>
                </div>
                <BadgeSelector
                  label=""
                  options={fieldValues.resourcesAvailable}
                  selectedValues={filters.resourcesNeeded}
                  allowEmptySelection
                  onToggle={(value) =>
                    handleMultiSelectFilter(
                      "resourcesNeeded",
                      value,
                      !filters.resourcesNeeded.includes(value),
                    )
                  }
                  labelFn={(value) => translateEnum("resources", value)}
                />
              </div>
              <div className="space-y-1.5">
                <div className="flex items-center gap-1.5">
                  <Tag className="h-3.5 w-3.5 text-muted-foreground" />
                  <span className="text-xs font-medium text-muted-foreground">
                    {t("library.topics")}
                  </span>
                </div>
                <BadgeSelector
                  label=""
                  options={filterOptions.topics}
                  selectedValues={filters.topics}
                  allowEmptySelection
                  onToggle={(value) =>
                    handleMultiSelectFilter(
                      "topics",
                      value,
                      !filters.topics.includes(value),
                    )
                  }
                  labelFn={(value) => translateEnum("topics", value)}
                />
              </div>
            </div>
          </div>

          <div className="border-t border-border" />

          <div className="space-y-2">
            <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wide">
              {t("library.energyAndCognition")}
            </p>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <div className="flex items-center gap-1.5">
                  <Brain className="h-3.5 w-3.5 text-muted-foreground" />
                  <span className="text-xs font-medium text-muted-foreground">
                    {t("library.mentalLoad")}
                  </span>
                </div>
                <BadgeSelector
                  label=""
                  options={filterOptions.mentalLoad}
                  selectedValues={filters.mentalLoad}
                  allowEmptySelection
                  onToggle={(value) =>
                    handleMultiSelectFilter(
                      "mentalLoad",
                      value,
                      !filters.mentalLoad.includes(value),
                    )
                  }
                  labelFn={(value) => translateEnum("energyLevel", value)}
                />
              </div>
              <div className="space-y-1.5">
                <div className="flex items-center gap-1.5">
                  <ActivityIcon className="h-3.5 w-3.5 text-muted-foreground" />
                  <span className="text-xs font-medium text-muted-foreground">
                    {t("library.physicalEnergy")}
                  </span>
                </div>
                <BadgeSelector
                  label=""
                  options={filterOptions.physicalEnergy}
                  selectedValues={filters.physicalEnergy}
                  allowEmptySelection
                  onToggle={(value) =>
                    handleMultiSelectFilter(
                      "physicalEnergy",
                      value,
                      !filters.physicalEnergy.includes(value),
                    )
                  }
                  labelFn={(value) => translateEnum("energyLevel", value)}
                />
              </div>
            </div>
          </div>
        </div>
      )}

      {error && (
        <Alert variant="destructive">
          <AlertDescription>{String(error)}</AlertDescription>
        </Alert>
      )}

      {showEmptyState ? (
        <div className="text-center py-12">
          <Heart className="mx-auto h-10 w-10 text-muted-foreground mb-3" />
          <h3 className="text-sm font-semibold text-foreground mb-1">
            {t("activityFavourites.noFavourites")}
          </h3>
          <p className="text-xs text-muted-foreground">
            {t("activityFavourites.noFavouritesDesc")}
          </p>
        </div>
      ) : (
        <>
          <div className="flex items-center justify-between">
            {isLoading ? (
              <div className="h-3 w-24 bg-muted/50 rounded animate-pulse" />
            ) : (
              <p className="text-xs text-muted-foreground tabular-nums">
                {total} {t("library.totalActivities")}
              </p>
            )}
            <ViewToggle value={viewMode} onChange={handleViewChange} />
          </div>

          {isLoading ? (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
              {[...Array(6)].map((_, index) => (
                <ActivityCardSkeleton key={index} />
              ))}
            </div>
          ) : favourites.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-10 text-center border border-border rounded-lg">
              <p className="text-sm text-muted-foreground">
                {t("activityFavourites.noResults")}
              </p>
            </div>
          ) : viewMode === "grid" ? (
            <>
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
                {favourites.map(({ favouriteId, activity }) => (
                  <ActivityCard
                    key={favouriteId}
                    activity={activity}
                    onClick={() => handleViewDetails(activity.id, activity)}
                    showRemoveButton
                    isRemoving={removingIds.has(activity.id)}
                    onRemove={() => removeFavourite(activity.id)}
                  />
                ))}
              </div>
              <PaginationBar
                currentPage={currentPage}
                totalPages={totalPages}
                totalItems={total}
                itemsPerPage={ITEMS_PER_PAGE}
                onPageChange={handlePageChange}
              />
            </>
          ) : (
            <div className="border border-border rounded-lg overflow-hidden">
              <div className="flex items-center gap-3 px-3 py-2 bg-muted/40 border-b border-border text-xs font-medium text-muted-foreground uppercase tracking-wide">
                <span className="flex-1">
                  {t("activityFavourites.nameHeader")}
                </span>
                <span className="w-[52px] shrink-0 hidden sm:block">
                  {t("activityFavourites.ageHeader")}
                </span>
                <span className="w-[68px] shrink-0 hidden sm:block">
                  {t("activityFavourites.durationHeader")}
                </span>
                <span className="w-[76px] shrink-0">
                  {t("activityFavourites.formatHeader")}
                </span>
                <span className="w-[100px] shrink-0 hidden lg:block">
                  {t("activityFavourites.dateHeader")}
                </span>
                <span className="w-[32px] shrink-0" />
              </div>

              <div className="divide-y divide-border">
                {favourites.map(
                  ({ favouriteId, favouriteCreatedAt, activity }) => {
                    const isRemoving = removingIds.has(activity.id);
                    return (
                      <div
                        key={favouriteId}
                        className="flex items-center gap-3 px-3 py-2.5 hover:bg-muted/30 transition-colors cursor-pointer"
                        onClick={() => handleViewDetails(activity.id, activity)}
                      >
                        <div className="flex-1 min-w-0">
                          <p className="text-sm font-medium truncate">
                            {activity.name}
                          </p>
                          {activity.source && (
                            <p className="text-xs text-muted-foreground truncate">
                              {activity.source}
                            </p>
                          )}
                        </div>

                        <div className="w-[52px] shrink-0 hidden sm:flex items-center gap-0.5 text-xs text-muted-foreground">
                          <Users className="h-3 w-3 shrink-0" />
                          {activity.ageMin}–{activity.ageMax}
                        </div>

                        <div className="w-[68px] shrink-0 hidden sm:flex items-center gap-0.5 text-xs text-muted-foreground">
                          <Clock className="h-3 w-3 shrink-0" />
                          {activity.durationMinMinutes}
                          {activity.durationMaxMinutes &&
                            `–${activity.durationMaxMinutes}`}
                          &nbsp;min
                        </div>

                        <div className="w-[76px] shrink-0">
                          <Badge
                            variant="secondary"
                            className="text-xs px-1.5 py-0 font-normal"
                          >
                            {translateEnum("format", activity.format)}
                          </Badge>
                        </div>

                        <div className="w-[100px] shrink-0 hidden lg:block text-xs text-muted-foreground tabular-nums">
                          {new Date(favouriteCreatedAt).toLocaleDateString()}
                        </div>

                        <div className="w-[32px] shrink-0 flex justify-end">
                          <Button
                            variant="ghost"
                            size="icon"
                            className="h-7 w-7 text-destructive"
                            onClick={(e) => {
                              e.stopPropagation();
                              removeFavourite(activity.id);
                            }}
                            disabled={isRemoving}
                            aria-label="Remove favourite"
                          >
                            <Trash2 className="h-3.5 w-3.5" />
                          </Button>
                        </div>
                      </div>
                    );
                  },
                )}
              </div>

              <div className="px-3">
                <PaginationBar
                  currentPage={currentPage}
                  totalPages={totalPages}
                  totalItems={total}
                  itemsPerPage={ITEMS_PER_PAGE}
                  onPageChange={handlePageChange}
                />
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );
};
