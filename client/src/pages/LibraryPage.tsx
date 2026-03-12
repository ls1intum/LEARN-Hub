import React, { useState, useMemo, useCallback } from "react";
import { useNavigate, Link } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Slider } from "@/components/ui/slider";
import { Card, CardContent } from "@/components/ui/card";
import { LoadingState, SkeletonGrid } from "@/components/ui/LoadingState";
import { ErrorDisplay } from "@/components/ui/ErrorDisplay";
import { BadgeSelector } from "@/components/ui/BadgeSelector";
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
  ChevronLeft,
  ChevronRight,
  Eye,
  BookOpen,
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
  const { user } = useAuth();
  const { fieldValues } = useFieldValues();
  const [currentPage, setCurrentPage] = useState(1);
  const [showFilters, setShowFilters] = useState(false);
  const [favouritedActivityIds, setFavouritedActivityIds] = useState<
    Set<string>
  >(new Set());

  const itemsPerPage = ACTIVITY_CONSTANTS.ITEMS_PER_PAGE;
  const isAdmin = user?.role === "ADMIN";

  // Filter options from field values
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

  // Form management for filters
  const filterForm = useForm({
    initialValues: initialFilterData,
    onSubmit: async () => {
      // This will be handled by the data fetch hook
    },
  });

  // Data fetching with automatic refetch on filter changes
  const fetchActivities = useCallback(async () => {
    const filters = filterForm.values;
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
        filters.physicalEnergy.length > 0
          ? filters.physicalEnergy
          : undefined,
      limit: itemsPerPage,
      offset: (currentPage - 1) * itemsPerPage,
    });
  }, [filterForm.values, currentPage, itemsPerPage]);

  const {
    data: activitiesData,
    isLoading,
    error,
    refetch,
  } = useDataFetch({
    fetchFn: fetchActivities,
    dependencies: [fetchActivities],
  });

  const activities = activitiesData?.activities || [];
  const total = activitiesData?.total || 0;

  // Fetch all favourited activities in bulk (avoid N+1 requests)
  React.useEffect(() => {
    const fetchFavourites = async () => {
      if (!user) {
        return;
      }

      try {
        const response = await apiService.getActivityFavourites();
        const favouritedIds = new Set(
          response.favourites.map((fav) => fav.activityId),
        );
        setFavouritedActivityIds(favouritedIds);
      } catch {
        // Silently fail - this is optional feature
        setFavouritedActivityIds(new Set());
      }
    };

    fetchFavourites();
  }, [user]);

  const handleFilterChange = (
    filterType: keyof FilterFormData,
    value: string | number,
  ) => {
    filterForm.setValue(
      filterType,
      value as FilterFormData[keyof FilterFormData],
    );
    setCurrentPage(1);
  };

  const handleMultiSelectFilter = (
    filterType: keyof FilterFormData,
    value: string,
    checked: boolean,
  ) => {
    const currentValues = filterForm.values[filterType] as string[];
    const newValues = checked
      ? [...currentValues, value]
      : currentValues.filter((item) => item !== value);

    filterForm.setValue(
      filterType,
      newValues as FilterFormData[keyof FilterFormData],
    );
    setCurrentPage(1);
  };

  const clearFilters = () => {
    filterForm.reset();
    setCurrentPage(1);
  };

  const totalPages = Math.ceil(total / itemsPerPage);

  return (
    <div className="w-full space-y-6 px-2 sm:px-4 lg:px-6">
      {/* Header */}
      <div className="text-center space-y-4">
        <div className="flex items-center justify-center gap-3">
          <div className="w-12 h-12 rounded-2xl bg-gradient-to-br from-primary to-primary/80 flex items-center justify-center shadow-lg">
            <BookOpen className="h-6 w-6 text-primary-foreground" />
          </div>
          <h2 className="text-4xl font-bold text-foreground">
            Activity Library
          </h2>
        </div>
        <p className="text-xl text-muted-foreground leading-relaxed max-w-2xl mx-auto">
          Browse, filter, and manage all available activities in your teaching
          toolkit.
        </p>

        {/* Action Buttons */}
        <div className="flex flex-col sm:flex-row gap-4 justify-center items-center">
          {isAdmin && (
            <Button asChild size="lg" className="h-12 px-8">
              <Link to="/upload">
                <Plus className="h-5 w-5 mr-2" />
                Add Activity
              </Link>
            </Button>
          )}
        </div>
      </div>

      <Card className="shadow-xl border-0 bg-gradient-to-br from-card to-card/50">
        <CardContent className="p-4 sm:p-6 lg:p-8">
          {/* Search and Quick Filters */}
          <div className="mb-6">
            <div className="flex flex-col sm:flex-row gap-4 mb-4">
              <div className="flex-1">
                <div className="relative">
                  <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                  <Input
                    placeholder="Search activities..."
                    value={filterForm.values.name}
                    onChange={(e) => handleFilterChange("name", e.target.value)}
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
                  {showFilters ? "Hide Filters" : "Show Filters"}
                </Button>
                <Button variant="outline" onClick={clearFilters}>
                  Clear All
                </Button>
              </div>
            </div>

            {/* Quick Stats */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-8">
              <div className="bg-primary/5 p-6 rounded-xl border border-primary/10">
                <div className="flex items-center">
                  <div className="p-3 bg-primary/15 rounded-xl">
                    <Plus className="h-6 w-6 text-primary" />
                  </div>
                  <div className="ml-4">
                    <p className="text-sm font-medium text-primary">
                      Total Activities
                    </p>
                    <p className="text-3xl font-bold text-primary">{total}</p>
                  </div>
                </div>
              </div>
              <div className="bg-success/5 p-6 rounded-xl border border-success/10">
                <div className="flex items-center">
                  <div className="p-3 bg-success/15 rounded-xl">
                    <Filter className="h-6 w-6 text-success" />
                  </div>
                  <div className="ml-4">
                    <p className="text-sm font-medium text-success">Showing</p>
                    <p className="text-3xl font-bold text-success">
                      {activities?.length || 0}
                    </p>
                  </div>
                </div>
              </div>
            </div>
          </div>

          {/* Advanced Filters */}
          {showFilters && (
            <div className="mb-8 p-6 bg-gradient-to-br from-muted/20 to-muted/10 rounded-xl border border-border/50 shadow-sm">
              <div className="flex justify-between items-center mb-8">
                <h3 className="text-xl font-semibold text-foreground">
                  Advanced Filters
                </h3>
              </div>

              {/* Range Filters Section */}
              <div className="mb-8">
                <h4 className="text-sm font-semibold text-muted-foreground uppercase tracking-wide mb-4">
                  Range Filters
                </h4>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  {/* Age Range */}
                  <div className="bg-card/50 border border-border/50 rounded-lg p-5 shadow-xs">
                    <div className="flex items-center gap-2 mb-1">
                      <Users className="h-4 w-4 text-primary" />
                      <Label className="font-semibold">Age Range</Label>
                    </div>
                    <div className="flex justify-between items-center mb-4">
                      <span className="text-sm text-muted-foreground">
                        Select age range for learners
                      </span>
                      <span className="text-lg font-bold text-primary">
                        {filterForm.values.ageMin} -{" "}
                        {filterForm.values.ageMax}
                      </span>
                    </div>
                    <Slider
                      value={[
                        filterForm.values.ageMin,
                        filterForm.values.ageMax,
                      ]}
                      onValueChange={(value) => {
                        handleFilterChange("ageMin", value[0]);
                        handleFilterChange("ageMax", value[1]);
                      }}
                      max={ACTIVITY_CONSTANTS.AGE_RANGE.MAX}
                      min={ACTIVITY_CONSTANTS.AGE_RANGE.MIN}
                      step={1}
                      className="w-full"
                    />
                  </div>

                  {/* Duration Range */}
                  <div className="bg-card/50 border border-border/50 rounded-lg p-5 shadow-xs">
                    <div className="flex items-center gap-2 mb-1">
                      <Clock className="h-4 w-4 text-primary" />
                      <Label className="font-semibold">
                        Duration (minutes)
                      </Label>
                    </div>
                    <div className="flex justify-between items-center mb-4">
                      <span className="text-sm text-muted-foreground">
                        Select activity duration
                      </span>
                      <span className="text-lg font-bold text-primary">
                        {filterForm.values.durationMin} -{" "}
                        {filterForm.values.durationMax}
                      </span>
                    </div>
                    <Slider
                      value={[
                        filterForm.values.durationMin,
                        filterForm.values.durationMax,
                      ]}
                      onValueChange={(value) => {
                        handleFilterChange("durationMin", value[0]);
                        handleFilterChange("durationMax", value[1]);
                      }}
                      max={ACTIVITY_CONSTANTS.DURATION_RANGE.MAX}
                      min={ACTIVITY_CONSTANTS.DURATION_RANGE.MIN}
                      step={ACTIVITY_CONSTANTS.DURATION_RANGE.STEP}
                      className="w-full"
                    />
                  </div>
                </div>
              </div>

              {/* Activity Characteristics Section */}
              <div className="mb-8">
                <h4 className="text-sm font-semibold text-muted-foreground uppercase tracking-wide mb-4">
                  Activity Characteristics
                </h4>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                  {/* Format */}
                  <div className="bg-card/50 border border-border/50 rounded-lg p-5">
                    <div className="flex items-center gap-2 mb-4">
                      <Grid3x3 className="h-4 w-4 text-primary" />
                      <Label className="font-semibold">Format</Label>
                    </div>
                    <BadgeSelector
                      label=""
                      options={filterOptions.format}
                      selectedValues={filterForm.values.format}
                      onToggle={(value) =>
                        handleMultiSelectFilter(
                          "format",
                          value,
                          !filterForm.values.format.includes(value),
                        )
                      }
                    />
                  </div>

                  {/* Bloom Level */}
                  <div className="bg-card/50 border border-border/50 rounded-lg p-5">
                    <div className="flex items-center gap-2 mb-4">
                      <GraduationCap className="h-4 w-4 text-primary" />
                      <Label className="font-semibold">Bloom's Level</Label>
                    </div>
                    <BadgeSelector
                      label=""
                      options={filterOptions.bloomLevel}
                      selectedValues={filterForm.values.bloomLevel}
                      onToggle={(value) =>
                        handleMultiSelectFilter(
                          "bloomLevel",
                          value,
                          !filterForm.values.bloomLevel.includes(value),
                        )
                      }
                    />
                  </div>

                  {/* Resources */}
                  <div className="bg-card/50 border border-border/50 rounded-lg p-5">
                    <div className="flex items-center gap-2 mb-4">
                      <Package className="h-4 w-4 text-primary" />
                      <Label className="font-semibold">Resources Needed</Label>
                    </div>
                    <BadgeSelector
                      label=""
                      options={filterOptions.resourcesAvailable}
                      selectedValues={filterForm.values.resourcesNeeded}
                      onToggle={(value) =>
                        handleMultiSelectFilter(
                          "resourcesNeeded",
                          value,
                          !filterForm.values.resourcesNeeded.includes(value),
                        )
                      }
                    />
                  </div>

                  {/* Topics */}
                  <div className="bg-card/50 border border-border/50 rounded-lg p-5">
                    <div className="flex items-center gap-2 mb-4">
                      <Tag className="h-4 w-4 text-primary" />
                      <Label className="font-semibold">Topics</Label>
                    </div>
                    <BadgeSelector
                      label=""
                      options={filterOptions.topics}
                      selectedValues={filterForm.values.topics}
                      onToggle={(value) =>
                        handleMultiSelectFilter(
                          "topics",
                          value,
                          !filterForm.values.topics.includes(value),
                        )
                      }
                    />
                  </div>
                </div>
              </div>

              {/* Teacher Context Section */}
              <div>
                <h4 className="text-sm font-semibold text-muted-foreground uppercase tracking-wide mb-4">
                  Teacher Context
                </h4>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                  {/* Mental Load */}
                  <div className="bg-card/50 border border-border/50 rounded-lg p-5">
                    <div className="flex items-center gap-2 mb-4">
                      <Brain className="h-4 w-4 text-primary" />
                      <Label className="font-semibold">Mental Load</Label>
                    </div>
                    <BadgeSelector
                      label=""
                      options={filterOptions.mentalLoad}
                      selectedValues={filterForm.values.mentalLoad}
                      onToggle={(value) =>
                        handleMultiSelectFilter(
                          "mentalLoad",
                          value,
                          !filterForm.values.mentalLoad.includes(value),
                        )
                      }
                    />
                  </div>

                  {/* Physical Energy */}
                  <div className="bg-card/50 border border-border/50 rounded-lg p-5">
                    <div className="flex items-center gap-2 mb-4">
                      <ActivityIcon className="h-4 w-4 text-primary" />
                      <Label className="font-semibold">Physical Energy</Label>
                    </div>
                    <BadgeSelector
                      label=""
                      options={filterOptions.physicalEnergy}
                      selectedValues={filterForm.values.physicalEnergy}
                      onToggle={(value) =>
                        handleMultiSelectFilter(
                          "physicalEnergy",
                          value,
                          !filterForm.values.physicalEnergy.includes(value),
                        )
                      }
                    />
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* Results Summary */}
          <div className="mb-8 p-6 bg-primary/5 rounded-xl border border-primary/10">
            <p className="text-primary font-medium">
              Showing {activities?.length || 0} of {total} activities
              {totalPages > 1 && ` (Page ${currentPage} of ${totalPages})`}
            </p>
          </div>

          {/* Error Display */}
          <ErrorDisplay error={error} onRetry={refetch} />

          {/* Activities Display */}
          <LoadingState isLoading={isLoading} fallback={<SkeletonGrid />}>
            {(activities?.length || 0) === 0 ? (
              <div className="text-center py-16">
                <div className="w-20 h-20 bg-muted/20 rounded-full flex items-center justify-center mx-auto mb-6">
                  <Filter className="h-10 w-10 text-muted-foreground" />
                </div>
                <h3 className="text-xl font-semibold text-foreground mb-3">
                  No activities found
                </h3>
                <p className="text-muted-foreground mb-8 leading-relaxed">
                  No activities match your current filters. Try adjusting your
                  search criteria.
                </p>
                <Button
                  variant="outline"
                  onClick={clearFilters}
                  size="lg"
                  className="h-12 px-8"
                >
                  Clear Filters
                </Button>
              </div>
            ) : (
              <>
                {/* Mobile Card View */}
                <div className="lg:hidden space-y-4">
                  {activities.map((activity) => (
                    <div
                      key={activity.id}
                      className="bg-card rounded-xl shadow-sm border border-border p-6"
                    >
                      <div className="flex items-start justify-between mb-4">
                        <div className="flex-1">
                          <h3 className="text-lg font-semibold text-foreground mb-1">
                            {activity.name}
                          </h3>
                          <p className="text-sm text-muted-foreground">
                            {activity.source}
                          </p>
                        </div>
                        <FavouriteButton
                          activityId={activity.id}
                          initialIsFavourited={favouritedActivityIds.has(
                            activity.id,
                          )}
                        />
                      </div>

                      <div className="grid grid-cols-2 gap-4 mb-4">
                        <div>
                          <span className="text-xs text-muted-foreground">
                            Age Range
                          </span>
                          <p className="text-sm font-medium">
                            {activity.ageMin}-{activity.ageMax}
                          </p>
                        </div>
                        <div>
                          <span className="text-xs text-muted-foreground">
                            Duration
                          </span>
                          <p className="text-sm font-medium">
                            {activity.durationMinMinutes}-
                            {activity.durationMaxMinutes} min
                          </p>
                        </div>
                      </div>

                      <div className="flex flex-wrap gap-2 mb-4">
                        <span className="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium bg-primary/10 text-primary border border-primary/20">
                          {activity.format}
                        </span>
                        {activity.topics?.slice(0, 2).map((topic, index) => (
                          <span
                            key={index}
                            className="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium bg-muted/30 text-foreground border border-border"
                          >
                            {topic}
                          </span>
                        ))}
                      </div>

                      <div className="flex items-center justify-end space-x-2 pt-4 border-t border-border">
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() =>
                            navigate(`/activity-details/${activity.id}`, {
                              state: { fromBrowser: true, activity },
                            })
                          }
                        >
                          <Eye className="h-3 w-3 mr-2" />
                          View Details
                        </Button>
                      </div>
                    </div>
                  ))}
                </div>

                {/* Desktop Table View */}
                <div className="hidden lg:block bg-card rounded-xl shadow-sm border border-border overflow-hidden">
                  <div className="px-4 py-3 border-b border-border bg-muted/20">
                    <h3 className="text-xl font-semibold text-card-foreground">
                      Activities
                    </h3>
                  </div>

                  <div className="overflow-x-auto">
                    <table
                      className="w-full divide-y divide-border"
                      style={{ minWidth: "600px" }}
                    >
                      <thead className="bg-muted/30">
                        <tr>
                          <th className="px-2 py-3 text-left text-sm font-semibold text-foreground min-w-[200px]">
                            Activity
                          </th>
                          <th className="px-2 py-3 text-left text-sm font-semibold text-foreground w-16">
                            Age
                          </th>
                          <th className="px-2 py-3 text-left text-sm font-semibold text-foreground w-16">
                            Format
                          </th>
                          <th className="px-2 py-3 text-left text-sm font-semibold text-foreground w-20">
                            Duration
                          </th>
                          <th className="px-2 py-3 text-left text-sm font-semibold text-foreground min-w-[120px]">
                            Topics
                          </th>
                          <th className="px-2 py-3 text-right text-sm font-semibold text-foreground w-24">
                            Actions
                          </th>
                        </tr>
                      </thead>
                      <tbody className="bg-card divide-y divide-border">
                        {activities.map((activity) => (
                          <tr
                            key={activity.id}
                            className="hover:bg-muted/30 transition-colors duration-150"
                          >
                            <td className="px-2 py-3">
                              <div>
                                <div className="text-sm font-semibold text-foreground line-clamp-2">
                                  {activity.name}
                                </div>
                                <div className="text-xs text-muted-foreground mt-1">
                                  {activity.source}
                                </div>
                                <div className="flex items-center gap-1 mt-1">
                                  <span className="inline-flex items-center gap-1 px-1 py-0.5 rounded-full text-xs font-medium bg-warning/10 text-warning border border-warning/20">
                                    <Brain className="h-2 w-2" />
                                    {activity.mentalLoad}
                                  </span>
                                  <span className="inline-flex items-center gap-1 px-1 py-0.5 rounded-full text-xs font-medium bg-success/10 text-success border border-success/20">
                                    <ActivityIcon className="h-2 w-2" />
                                    {activity.physicalEnergy}
                                  </span>
                                </div>
                              </div>
                            </td>
                            <td className="px-2 py-3 whitespace-nowrap">
                              <div className="text-sm font-medium text-foreground">
                                {activity.ageMin}-{activity.ageMax}
                              </div>
                            </td>
                            <td className="px-2 py-3 whitespace-nowrap">
                              <span className="inline-flex items-center px-1.5 py-0.5 rounded-full text-xs font-medium bg-primary/10 text-primary border border-primary/20">
                                {activity.format}
                              </span>
                            </td>
                            <td className="px-2 py-3 whitespace-nowrap">
                              <div className="text-sm font-medium text-foreground">
                                {activity.durationMinMinutes}-
                                {activity.durationMaxMinutes}m
                              </div>
                            </td>
                            <td className="px-2 py-3">
                              <div className="flex flex-wrap gap-1">
                                {activity.topics
                                  ?.slice(0, 1)
                                  .map((topic, index) => (
                                    <span
                                      key={index}
                                      className="inline-flex items-center px-1 py-0.5 rounded-full text-xs font-medium bg-muted/30 text-foreground border border-border"
                                    >
                                      {topic}
                                    </span>
                                  ))}
                                {activity.topics &&
                                  activity.topics.length > 1 && (
                                    <span className="inline-flex items-center px-1 py-0.5 rounded-full text-xs font-medium bg-muted/20 text-muted-foreground">
                                      +{activity.topics.length - 1}
                                    </span>
                                  )}
                              </div>
                            </td>
                            <td className="px-2 py-3 whitespace-nowrap text-right text-sm font-medium">
                              <div className="flex items-center justify-end space-x-2">
                                <FavouriteButton
                                  activityId={activity.id}
                                  initialIsFavourited={favouritedActivityIds.has(
                                    activity.id,
                                  )}
                                />
                                <Button
                                  variant="outline"
                                  size="sm"
                                  onClick={() =>
                                    navigate(
                                      `/activity-details/${activity.id}`,
                                      {
                                        state: { fromBrowser: true, activity },
                                      },
                                    )
                                  }
                                >
                                  <Eye className="h-3 w-3 mr-2" />
                                  View Details
                                </Button>
                              </div>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>

                {/* Pagination */}
                {totalPages > 1 && (
                  <div className="flex justify-center items-center gap-4 mt-6">
                    <Button
                      variant="outline"
                      onClick={() =>
                        setCurrentPage((prev) => Math.max(1, prev - 1))
                      }
                      disabled={currentPage === 1}
                      className="flex items-center gap-2"
                    >
                      <ChevronLeft className="h-4 w-4" />
                      Previous
                    </Button>
                    <span className="text-muted-foreground">
                      Page {currentPage} of {totalPages}
                    </span>
                    <Button
                      variant="outline"
                      onClick={() =>
                        setCurrentPage((prev) => Math.min(totalPages, prev + 1))
                      }
                      disabled={currentPage === totalPages}
                      className="flex items-center gap-2"
                    >
                      Next
                      <ChevronRight className="h-4 w-4" />
                    </Button>
                  </div>
                )}
              </>
            )}
          </LoadingState>
        </CardContent>
      </Card>
    </div>
  );
};
