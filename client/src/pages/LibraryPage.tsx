import React, { useState, useMemo, useCallback, useEffect } from "react";
import {
  useNavigate,
  Link,
  useLocation,
  useSearchParams,
} from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Slider } from "@/components/ui/slider";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { LoadingState } from "@/components/ui/LoadingState";
import { ErrorDisplay } from "@/components/ui/ErrorDisplay";
import { BadgeSelector } from "@/components/ui/BadgeSelector";
import { PaginationBar } from "@/components/ui/PaginationBar";
import { useAuth } from "@/hooks/useAuth";
import { useDataFetch } from "@/hooks/useDataFetch";
import { useForm } from "@/hooks/useForm";
import { apiService } from "@/services/apiService";
import type { FilterOptions } from "@/types/activity";
import { ACTIVITY_CONSTANTS } from "@/constants/activity";
import { useFieldValues } from "@/hooks/useFieldValues";
import {
  Search,
  Filter,
  Plus,
  Brain,
  Activity as ActivityIcon,
  Users,
  Clock,
  Grid3x3,
  GraduationCap,
  Package,
  Tag,
} from "lucide-react";
import { FavouriteButton } from "@/components/favourites/FavouriteButton";
import { ActivityCard, ActivityCardSkeleton } from "@/components/ActivityCard";
import { ViewToggle, type ViewMode } from "@/components/ui/ViewToggle";
import { useTranslation } from "react-i18next";
import { useTranslateEnum } from "@/hooks/useTranslateEnum";
import { useRestoreScroll } from "@/hooks/useRestoreScroll";
import { getAppScrollTop, setAppScrollTop } from "@/utils/scroll";

interface LibraryLocationState {
  restoreScrollY?: number;
}

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

export const LibraryPage: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const [searchParams, setSearchParams] = useSearchParams();
  const { user } = useAuth();
  const { fieldValues } = useFieldValues();
  const { t } = useTranslation();
  const restoreScrollY = (location.state as LibraryLocationState | null)
    ?.restoreScrollY;

  const parseNumberParam = useCallback(
    (key: string, fallback: number) => {
      const rawValue = searchParams.get(key);
      if (rawValue === null || rawValue === "") return fallback;
      const value = Number(rawValue);
      return Number.isFinite(value) ? value : fallback;
    },
    [searchParams],
  );
  const parseArrayParam = useCallback(
    (key: string) => searchParams.get(key)?.split(",").filter(Boolean) ?? [],
    [searchParams],
  );
  const initialFilters = useMemo<FilterFormData>(
    () => ({
      name: searchParams.get("name") ?? initialFilterData.name,
      ageMin: parseNumberParam("ageMin", initialFilterData.ageMin),
      ageMax: parseNumberParam("ageMax", initialFilterData.ageMax),
      format: parseArrayParam("format"),
      bloomLevel: parseArrayParam("bloomLevel"),
      resourcesNeeded: parseArrayParam("resourcesNeeded"),
      topics: parseArrayParam("topics"),
      mentalLoad: parseArrayParam("mentalLoad"),
      physicalEnergy: parseArrayParam("physicalEnergy"),
      durationMin: parseNumberParam(
        "durationMin",
        initialFilterData.durationMin,
      ),
      durationMax: parseNumberParam(
        "durationMax",
        initialFilterData.durationMax,
      ),
    }),
    [parseArrayParam, parseNumberParam, searchParams],
  );

  const translateEnum = useTranslateEnum();

  const [currentPage, setCurrentPage] = useState(() =>
    parseNumberParam("page", 1),
  );
  const [showFilters, setShowFilters] = useState(
    () => searchParams.get("showFilters") === "true",
  );
  const [viewMode, setViewMode] = useState<ViewMode>(
    () => (localStorage.getItem("view-library") as ViewMode) ?? "grid",
  );
  const handleViewChange = (mode: ViewMode) => {
    setViewMode(mode);
    localStorage.setItem("view-library", mode);
  };
  const scrollToTop = useCallback(() => {
    if (typeof restoreScrollY === "number") return;
    setAppScrollTop(0);
  }, [restoreScrollY]);

  const itemsPerPage = ACTIVITY_CONSTANTS.ITEMS_PER_PAGE;
  const isAdmin = user?.role === "ADMIN";

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

  const filterForm = useForm({
    initialValues: initialFilters,
    onSubmit: async () => {},
  });
  const [appliedFilters, setAppliedFilters] =
    useState<FilterFormData>(initialFilters);

  const applyFiltersImmediately = useCallback((nextFilters: FilterFormData) => {
    setAppliedFilters(nextFilters);
    setCurrentPage(1);
  }, []);

  const fetchActivities = useCallback(async () => {
    const filters = appliedFilters;
    return await apiService.getActivities({
      name: filters.name || undefined,
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
      limit: itemsPerPage,
      offset: (currentPage - 1) * itemsPerPage,
    });
  }, [appliedFilters, currentPage, itemsPerPage]);

  const {
    data: activitiesData,
    isLoading,
    error,
    refetch,
  } = useDataFetch({ fetchFn: fetchActivities });

  const activities = activitiesData?.activities || [];
  const total = activitiesData?.total || 0;

  const handleFilterChange = (
    filterType: keyof FilterFormData,
    value: string | number,
  ) => {
    scrollToTop();
    const nextFilters = {
      ...filterForm.values,
      [filterType]: value,
    } as FilterFormData;
    filterForm.setValue(
      filterType,
      value as FilterFormData[keyof FilterFormData],
    );
    applyFiltersImmediately(nextFilters);
  };

  const handleMultiSelectFilter = (
    filterType: keyof FilterFormData,
    value: string,
    checked: boolean,
  ) => {
    scrollToTop();
    const currentValues = filterForm.values[filterType] as string[];
    const newValues = checked
      ? [...currentValues, value]
      : currentValues.filter((item) => item !== value);
    const nextFilters = {
      ...filterForm.values,
      [filterType]: newValues,
    } as FilterFormData;
    filterForm.setValue(
      filterType,
      newValues as FilterFormData[keyof FilterFormData],
    );
    applyFiltersImmediately(nextFilters);
  };

  const handleRangeFilterChange = useCallback(
    (
      minField: "ageMin" | "durationMin",
      maxField: "ageMax" | "durationMax",
      values: number[],
    ) => {
      scrollToTop();
      const [minValue, maxValue] = values;
      filterForm.setValues({
        [minField]: minValue,
        [maxField]: maxValue,
      } as Partial<FilterFormData>);
    },
    [filterForm, scrollToTop],
  );

  const handleRangeFilterCommit = useCallback(
    (
      minField: "ageMin" | "durationMin",
      maxField: "ageMax" | "durationMax",
      values: number[],
    ) => {
      const [minValue, maxValue] = values;
      applyFiltersImmediately({
        ...filterForm.values,
        [minField]: minValue,
        [maxField]: maxValue,
      } as FilterFormData);
    },
    [applyFiltersImmediately, filterForm.values],
  );

  const handleClearFilters = useCallback(() => {
    scrollToTop();
    filterForm.setValues(initialFilterData);
    applyFiltersImmediately(initialFilterData);
    setShowFilters(false);
  }, [applyFiltersImmediately, filterForm, scrollToTop]);

  const activeFilters = appliedFilters;
  const clearFilters = () => handleClearFilters();
  const totalPages = Math.ceil(total / itemsPerPage);

  const activeFilterCount = useMemo(() => {
    let count = 0;
    if (appliedFilters.name) count++;
    if (
      appliedFilters.ageMin !== initialFilterData.ageMin ||
      appliedFilters.ageMax !== initialFilterData.ageMax
    )
      count++;
    if (
      appliedFilters.durationMin !== initialFilterData.durationMin ||
      appliedFilters.durationMax !== initialFilterData.durationMax
    )
      count++;
    if (appliedFilters.format.length > 0) count++;
    if (appliedFilters.bloomLevel.length > 0) count++;
    if (appliedFilters.resourcesNeeded.length > 0) count++;
    if (appliedFilters.topics.length > 0) count++;
    if (appliedFilters.mentalLoad.length > 0) count++;
    if (appliedFilters.physicalEnergy.length > 0) count++;
    return count;
  }, [appliedFilters]);

  useEffect(() => {
    if (currentPage > totalPages && totalPages > 0) setCurrentPage(totalPages);
  }, [currentPage, totalPages]);

  useEffect(() => {
    const params = new URLSearchParams();
    if (activeFilters.name) params.set("name", activeFilters.name);
    if (activeFilters.ageMin !== initialFilterData.ageMin)
      params.set("ageMin", String(activeFilters.ageMin));
    if (activeFilters.ageMax !== initialFilterData.ageMax)
      params.set("ageMax", String(activeFilters.ageMax));
    if (activeFilters.durationMin !== initialFilterData.durationMin)
      params.set("durationMin", String(activeFilters.durationMin));
    if (activeFilters.durationMax !== initialFilterData.durationMax)
      params.set("durationMax", String(activeFilters.durationMax));
    const setArrayParam = (key: string, values: string[]) => {
      if (values.length > 0) params.set(key, values.join(","));
    };
    setArrayParam("format", activeFilters.format);
    setArrayParam("bloomLevel", activeFilters.bloomLevel);
    setArrayParam("resourcesNeeded", activeFilters.resourcesNeeded);
    setArrayParam("topics", activeFilters.topics);
    setArrayParam("mentalLoad", activeFilters.mentalLoad);
    setArrayParam("physicalEnergy", activeFilters.physicalEnergy);
    if (currentPage > 1) params.set("page", String(currentPage));
    if (showFilters) params.set("showFilters", "true");
    if (params.toString() !== searchParams.toString())
      setSearchParams(params, { replace: true });
  }, [activeFilters, currentPage, searchParams, setSearchParams, showFilters]);

  useRestoreScroll(!!activitiesData && !isLoading);

  return (
    <div className="w-full space-y-4 py-6">
      {/* Header */}
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl sm:text-3xl font-bold tracking-tight text-foreground">
            {t("library.title")}
          </h1>
          <p className="text-sm text-muted-foreground mt-0.5">
            {t("library.subtitle")}
          </p>
        </div>
        {isAdmin && (
          <Button asChild size="sm" className="shrink-0">
            <Link to="/upload">
              <Plus className="h-3.5 w-3.5 mr-1.5" />
              {t("library.addActivity")}
            </Link>
          </Button>
        )}
      </div>

      {/* Toolbar */}
      <div className="flex items-center gap-2">
        <div className="relative flex-1 min-w-0">
          <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-muted-foreground pointer-events-none" />
          <Input
            placeholder={t("library.searchPlaceholder")}
            value={filterForm.values.name}
            onChange={(e) => handleFilterChange("name", e.target.value)}
            className="pl-8 h-8 text-sm w-full"
          />
        </div>
        <Button
          variant="outline"
          size="sm"
          className="h-8 gap-1.5 shrink-0"
          onClick={() => setShowFilters(!showFilters)}
        >
          <Filter className="h-3.5 w-3.5" />
          {showFilters ? t("library.hideFilters") : t("library.showFilters")}
          {activeFilterCount > 0 && (
            <span className="inline-flex items-center justify-center rounded-full bg-primary text-primary-foreground text-[10px] font-semibold w-4 h-4 leading-none">
              {activeFilterCount}
            </span>
          )}
        </Button>
        <Button
          variant="ghost"
          size="sm"
          className="h-8 text-muted-foreground shrink-0"
          onClick={clearFilters}
          disabled={activeFilterCount === 0}
        >
          {t("library.clearFilters")}
        </Button>
      </div>

      {/* Filter Panel */}
      {showFilters && (
        <div className="border border-border rounded-lg p-4 space-y-5 bg-muted/20">
          {/* Range Filters */}
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
                  {filterForm.values.ageMin}–{filterForm.values.ageMax}
                </span>
              </div>
              <Slider
                value={[filterForm.values.ageMin, filterForm.values.ageMax]}
                onValueChange={(v) =>
                  handleRangeFilterChange("ageMin", "ageMax", v)
                }
                onValueCommit={(v) =>
                  handleRangeFilterCommit("ageMin", "ageMax", v)
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
                  {filterForm.values.durationMin}–
                  {filterForm.values.durationMax}m
                </span>
              </div>
              <Slider
                value={[
                  filterForm.values.durationMin,
                  filterForm.values.durationMax,
                ]}
                onValueChange={(v) =>
                  handleRangeFilterChange("durationMin", "durationMax", v)
                }
                onValueCommit={(v) =>
                  handleRangeFilterCommit("durationMin", "durationMax", v)
                }
                max={ACTIVITY_CONSTANTS.DURATION_RANGE.MAX}
                min={ACTIVITY_CONSTANTS.DURATION_RANGE.MIN}
                step={ACTIVITY_CONSTANTS.DURATION_RANGE.STEP}
              />
            </div>
          </div>

          <div className="border-t border-border" />

          {/* Activity Characteristics */}
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
                  selectedValues={filterForm.values.format}
                  allowEmptySelection
                  onToggle={(v) =>
                    handleMultiSelectFilter(
                      "format",
                      v,
                      !filterForm.values.format.includes(v),
                    )
                  }
                  labelFn={(v) => translateEnum("format", v)}
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
                  selectedValues={filterForm.values.bloomLevel}
                  allowEmptySelection
                  onToggle={(v) =>
                    handleMultiSelectFilter(
                      "bloomLevel",
                      v,
                      !filterForm.values.bloomLevel.includes(v),
                    )
                  }
                  labelFn={(v) => translateEnum("bloomLevel", v)}
                />
              </div>
              <div className="space-y-1.5">
                <div className="flex items-center gap-1.5">
                  <Package className="h-3.5 w-3.5 text-muted-foreground" />
                  <span className="text-xs font-medium text-muted-foreground">
                    {t("library.resources")}
                  </span>
                </div>
                <BadgeSelector
                  label=""
                  options={filterOptions.resourcesAvailable}
                  selectedValues={filterForm.values.resourcesNeeded}
                  allowEmptySelection
                  onToggle={(v) =>
                    handleMultiSelectFilter(
                      "resourcesNeeded",
                      v,
                      !filterForm.values.resourcesNeeded.includes(v),
                    )
                  }
                  labelFn={(v) => translateEnum("resources", v)}
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
                  selectedValues={filterForm.values.topics}
                  allowEmptySelection
                  onToggle={(v) =>
                    handleMultiSelectFilter(
                      "topics",
                      v,
                      !filterForm.values.topics.includes(v),
                    )
                  }
                  labelFn={(v) => translateEnum("topics", v)}
                />
              </div>
            </div>
          </div>

          <div className="border-t border-border" />

          {/* Teacher Context */}
          <div className="space-y-2">
            <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wide">
              {t("library.teacherContext")}
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
                  selectedValues={filterForm.values.mentalLoad}
                  allowEmptySelection
                  onToggle={(v) =>
                    handleMultiSelectFilter(
                      "mentalLoad",
                      v,
                      !filterForm.values.mentalLoad.includes(v),
                    )
                  }
                  labelFn={(v) => translateEnum("energy", v)}
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
                  selectedValues={filterForm.values.physicalEnergy}
                  allowEmptySelection
                  onToggle={(v) =>
                    handleMultiSelectFilter(
                      "physicalEnergy",
                      v,
                      !filterForm.values.physicalEnergy.includes(v),
                    )
                  }
                  labelFn={(v) => translateEnum("energy", v)}
                />
              </div>
            </div>
          </div>
        </div>
      )}

      {!isLoading && (
        <div className="flex items-center justify-between">
          <p className="text-xs text-muted-foreground tabular-nums">
            {total} {t("library.totalActivities")}
          </p>
          <ViewToggle value={viewMode} onChange={handleViewChange} />
        </div>
      )}

      <ErrorDisplay error={error} onRetry={refetch} />

      {/* Activity List */}
      <LoadingState
        isLoading={isLoading}
        fallback={
          viewMode === "grid" ? (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
              {Array.from({ length: 6 }).map((_, i) => (
                <ActivityCardSkeleton key={i} />
              ))}
            </div>
          ) : (
            <div className="border border-border rounded-lg overflow-hidden">
              <div className="flex items-center gap-2 px-3 py-2 bg-muted/30 border-b border-border">
                <Skeleton className="h-3 flex-1 max-w-[180px]" />
                <Skeleton className="h-3 w-[52px] hidden sm:block" />
                <Skeleton className="h-3 w-[68px] hidden sm:block" />
                <Skeleton className="h-3 w-[76px]" />
                <Skeleton className="h-3 w-[84px] hidden md:block" />
                <Skeleton className="h-3 w-[64px]" />
              </div>
              <div className="divide-y divide-border">
                {Array.from({ length: 8 }).map((_, i) => (
                  <div key={i} className="flex items-center gap-2 px-3 py-2.5">
                    <div className="flex-1 space-y-1.5">
                      <Skeleton className="h-4 max-w-[220px]" />
                      <Skeleton className="h-3 max-w-[160px]" />
                    </div>
                    <Skeleton className="h-4 w-[52px] hidden sm:block" />
                    <Skeleton className="h-4 w-[68px] hidden sm:block" />
                    <Skeleton className="h-5 w-[76px] rounded-full" />
                    <Skeleton className="h-5 w-[84px] rounded-full hidden md:block" />
                    <Skeleton className="h-7 w-7 rounded ml-auto" />
                  </div>
                ))}
              </div>
            </div>
          )
        }
      >
        {activities.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-16 border border-border rounded-lg">
            <Filter className="h-8 w-8 text-muted-foreground mb-3" />
            <p className="text-sm font-medium mb-1">
              {t("library.noActivities")}
            </p>
            <p className="text-xs text-muted-foreground mb-4 text-center max-w-xs">
              {t("library.noActivitiesDesc")}
            </p>
            <Button variant="outline" size="sm" onClick={clearFilters}>
              {t("library.clearFilters")}
            </Button>
          </div>
        ) : (
          <>
            {viewMode === "grid" ? (
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
                {activities.map((activity) => (
                  <ActivityCard
                    key={activity.id}
                    activity={activity}
                    onClick={() =>
                      navigate(`/activity-details/${activity.id}`, {
                        state: {
                          activity,
                          backTo: `${location.pathname}${location.search}`,
                          restoreScrollY: getAppScrollTop(),
                        },
                      })
                    }
                  />
                ))}
              </div>
            ) : (
              <div className="border border-border rounded-lg overflow-hidden">
                {/* Column headers */}
                <div className="flex items-center gap-2 px-3 py-2 bg-muted/30 border-b border-border">
                  <span className="flex-1 text-xs font-medium text-muted-foreground">
                    {t("library.colActivity")}
                  </span>
                  <span className="w-[52px] text-xs font-medium text-muted-foreground hidden sm:block shrink-0">
                    {t("library.colAge")}
                  </span>
                  <span className="w-[68px] text-xs font-medium text-muted-foreground hidden sm:block shrink-0">
                    {t("library.colDuration")}
                  </span>
                  <span className="w-[76px] text-xs font-medium text-muted-foreground shrink-0">
                    {t("library.colFormat")}
                  </span>
                  <span className="w-[84px] text-xs font-medium text-muted-foreground hidden md:block shrink-0">
                    {t("library.colTopics")}
                  </span>
                  <span className="w-[64px] text-right text-xs font-medium text-muted-foreground shrink-0">
                    {t("library.colActions")}
                  </span>
                </div>

                {/* Rows */}
                <div className="divide-y divide-border">
                  {activities.map((activity) => (
                    <div
                      key={activity.id}
                      className="flex items-center gap-2 px-3 py-2.5 hover:bg-muted/40 transition-colors cursor-pointer"
                      onClick={() =>
                        navigate(`/activity-details/${activity.id}`, {
                          state: {
                            activity,
                            backTo: `${location.pathname}${location.search}`,
                            restoreScrollY: getAppScrollTop(),
                          },
                        })
                      }
                    >
                      <div className="flex-1 min-w-0">
                        <p className="text-sm font-medium text-foreground truncate leading-snug">
                          {activity.name}
                        </p>
                        {activity.source && (
                          <p className="text-xs text-muted-foreground truncate leading-snug">
                            {activity.source}
                          </p>
                        )}
                      </div>
                      <span className="w-[52px] text-xs text-muted-foreground tabular-nums hidden sm:block shrink-0">
                        {activity.ageMin}–{activity.ageMax}
                      </span>
                      <span className="w-[68px] text-xs text-muted-foreground tabular-nums hidden sm:block shrink-0">
                        {activity.durationMinMinutes}–
                        {activity.durationMaxMinutes}m
                      </span>
                      <div className="w-[76px] shrink-0">
                        <Badge
                          variant="secondary"
                          className="text-[11px] px-1.5 py-0 font-normal truncate max-w-full"
                        >
                          {translateEnum("format", activity.format)}
                        </Badge>
                      </div>
                      <div className="w-[84px] hidden md:flex items-center gap-1 shrink-0 min-w-0">
                        {activity.topics?.slice(0, 1).map((topic) => (
                          <Badge
                            key={topic}
                            variant="outline"
                            className="text-[11px] px-1.5 py-0 font-normal truncate max-w-full"
                          >
                            {translateEnum("topics", topic)}
                          </Badge>
                        ))}
                        {activity.topics && activity.topics.length > 1 && (
                          <span className="text-[11px] text-muted-foreground shrink-0">
                            +{activity.topics.length - 1}
                          </span>
                        )}
                      </div>
                      <div
                        className="w-[64px] flex items-center justify-end shrink-0"
                        onClick={(e) => e.stopPropagation()}
                      >
                        <FavouriteButton
                          activityId={activity.id}
                          variant="ghost"
                          size="icon"
                          className="h-7 w-7"
                          initialIsFavourited={activity.isFavourited ?? false}
                        />
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}

            <PaginationBar
              currentPage={currentPage}
              totalPages={totalPages}
              totalItems={total}
              itemsPerPage={itemsPerPage}
              onPageChange={(page) => {
                scrollToTop();
                setCurrentPage(page);
              }}
            />
          </>
        )}
      </LoadingState>
    </div>
  );
};
