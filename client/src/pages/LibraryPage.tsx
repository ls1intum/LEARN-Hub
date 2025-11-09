import React, { useState, useMemo, useCallback } from "react";
import { useNavigate, Link } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Checkbox } from "@/components/ui/checkbox";
import { Slider } from "@/components/ui/slider";
import { Card, CardContent } from "@/components/ui/card";
import { LoadingState, SkeletonGrid } from "@/components/ui/LoadingState";
import { ErrorDisplay } from "@/components/ui/ErrorDisplay";
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
} from "lucide-react";
import { FavouriteButton } from "@/components/favourites/FavouriteButton";

interface FilterFormData {
  name: string;
  age_min: number;
  age_max: number;
  format: string[];
  bloom_level: string[];
  resources_needed: string[];
  topics: string[];
  mental_load: string[];
  physical_energy: string[];
  duration_min: number;
  duration_max: number;
}

const initialFilterData: FilterFormData = {
  name: "",
  age_min: 0,
  age_max: 15,
  format: [],
  bloom_level: [],
  resources_needed: [],
  topics: [],
  mental_load: [],
  physical_energy: [],
  duration_min: 0,
  duration_max: 120,
};

export const LibraryPage: React.FC = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const { fieldValues } = useFieldValues();
  const [currentPage, setCurrentPage] = useState(1);
  const [showFilters, setShowFilters] = useState(false);
  const [favouritedActivityIds, setFavouritedActivityIds] = useState<
    Set<number>
  >(new Set());

  const itemsPerPage = ACTIVITY_CONSTANTS.ITEMS_PER_PAGE;
  const isAdmin = user?.role === "ADMIN";

  // Filter options from field values
  const filterOptions: FilterOptions = useMemo(
    () => ({
      format: fieldValues.format,
      resources_available: fieldValues.resources_available,
      bloom_level: fieldValues.bloom_level,
      topics: fieldValues.topics,
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
      age_min: filters.age_min > 0 ? filters.age_min : undefined,
      age_max: filters.age_max < 15 ? filters.age_max : undefined,
      duration_min: filters.duration_min > 0 ? filters.duration_min : undefined,
      duration_max:
        filters.duration_max < 120 ? filters.duration_max : undefined,
      format: filters.format.length > 0 ? filters.format : undefined,
      bloom_level:
        filters.bloom_level.length > 0 ? filters.bloom_level : undefined,
      resources_needed:
        filters.resources_needed.length > 0
          ? filters.resources_needed
          : undefined,
      topics: filters.topics.length > 0 ? filters.topics : undefined,
      mental_load:
        filters.mental_load.length > 0 ? filters.mental_load : undefined,
      physical_energy:
        filters.physical_energy.length > 0
          ? filters.physical_energy
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
          response.favourites.map((fav) => fav.activity_id),
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
            <div className="mb-8 p-6 bg-muted/30 rounded-xl border border-border">
              <div className="flex justify-between items-center mb-6">
                <h3 className="text-xl font-semibold text-foreground">
                  Advanced Filters
                </h3>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
                {/* Age Range */}
                <div>
                  <Label>
                    Age Range: {filterForm.values.age_min} -{" "}
                    {filterForm.values.age_max}
                  </Label>
                  <div className="space-y-2">
                    <Slider
                      value={[
                        filterForm.values.age_min,
                        filterForm.values.age_max,
                      ]}
                      onValueChange={(value) => {
                        handleFilterChange("age_min", value[0]);
                        handleFilterChange("age_max", value[1]);
                      }}
                      max={ACTIVITY_CONSTANTS.AGE_RANGE.MAX}
                      min={ACTIVITY_CONSTANTS.AGE_RANGE.MIN}
                      step={1}
                      className="w-full"
                    />
                  </div>
                </div>

                {/* Duration Range */}
                <div>
                  <Label>
                    Duration (minutes): {filterForm.values.duration_min} -{" "}
                    {filterForm.values.duration_max}
                  </Label>
                  <div className="space-y-2">
                    <Slider
                      value={[
                        filterForm.values.duration_min,
                        filterForm.values.duration_max,
                      ]}
                      onValueChange={(value) => {
                        handleFilterChange("duration_min", value[0]);
                        handleFilterChange("duration_max", value[1]);
                      }}
                      max={ACTIVITY_CONSTANTS.DURATION_RANGE.MAX}
                      min={ACTIVITY_CONSTANTS.DURATION_RANGE.MIN}
                      step={ACTIVITY_CONSTANTS.DURATION_RANGE.STEP}
                      className="w-full"
                    />
                  </div>
                </div>

                {/* Format */}
                <div>
                  <Label>Format</Label>
                  <div className="space-y-2 max-h-32 overflow-y-auto">
                    {filterOptions.format.map((format) => (
                      <div key={format} className="flex items-center space-x-2">
                        <Checkbox
                          id={`format-${format}`}
                          checked={filterForm.values.format.includes(format)}
                          onCheckedChange={(checked) =>
                            handleMultiSelectFilter(
                              "format",
                              format,
                              checked as boolean,
                            )
                          }
                        />
                        <Label htmlFor={`format-${format}`} className="text-sm">
                          {format}
                        </Label>
                      </div>
                    ))}
                  </div>
                </div>

                {/* Bloom Level */}
                <div>
                  <Label>Bloom's Taxonomy Level</Label>
                  <div className="space-y-2 max-h-32 overflow-y-auto">
                    {filterOptions.bloom_level.map((level) => (
                      <div key={level} className="flex items-center space-x-2">
                        <Checkbox
                          id={`bloom-${level}`}
                          checked={filterForm.values.bloom_level.includes(
                            level,
                          )}
                          onCheckedChange={(checked) =>
                            handleMultiSelectFilter(
                              "bloom_level",
                              level,
                              checked as boolean,
                            )
                          }
                        />
                        <Label htmlFor={`bloom-${level}`} className="text-sm">
                          {level}
                        </Label>
                      </div>
                    ))}
                  </div>
                </div>

                {/* Resources */}
                <div>
                  <Label>Resources Needed</Label>
                  <div className="space-y-2 max-h-32 overflow-y-auto">
                    {filterOptions.resources_available.map((resource) => (
                      <div
                        key={resource}
                        className="flex items-center space-x-2"
                      >
                        <Checkbox
                          id={`resource-${resource}`}
                          checked={filterForm.values.resources_needed.includes(
                            resource,
                          )}
                          onCheckedChange={(checked) =>
                            handleMultiSelectFilter(
                              "resources_needed",
                              resource,
                              checked as boolean,
                            )
                          }
                        />
                        <Label
                          htmlFor={`resource-${resource}`}
                          className="text-sm"
                        >
                          {resource}
                        </Label>
                      </div>
                    ))}
                  </div>
                </div>

                {/* Topics */}
                <div>
                  <Label>Topics</Label>
                  <div className="space-y-2 max-h-32 overflow-y-auto">
                    {filterOptions.topics.map((topic) => (
                      <div key={topic} className="flex items-center space-x-2">
                        <Checkbox
                          id={`topic-${topic}`}
                          checked={filterForm.values.topics.includes(topic)}
                          onCheckedChange={(checked) =>
                            handleMultiSelectFilter(
                              "topics",
                              topic,
                              checked as boolean,
                            )
                          }
                        />
                        <Label htmlFor={`topic-${topic}`} className="text-sm">
                          {topic}
                        </Label>
                      </div>
                    ))}
                  </div>
                </div>

                {/* Mental Load */}
                <div>
                  <Label>Mental Load</Label>
                  <div className="space-y-2">
                    {ACTIVITY_CONSTANTS.ENERGY_LEVELS.map((level) => (
                      <div key={level} className="flex items-center space-x-2">
                        <Checkbox
                          id={`mental-${level}`}
                          checked={filterForm.values.mental_load.includes(
                            level,
                          )}
                          onCheckedChange={(checked) =>
                            handleMultiSelectFilter(
                              "mental_load",
                              level,
                              checked as boolean,
                            )
                          }
                        />
                        <Label
                          htmlFor={`mental-${level}`}
                          className="text-sm capitalize"
                        >
                          {level}
                        </Label>
                      </div>
                    ))}
                  </div>
                </div>

                {/* Physical Energy */}
                <div>
                  <Label>Physical Energy</Label>
                  <div className="space-y-2">
                    {ACTIVITY_CONSTANTS.ENERGY_LEVELS.map((level) => (
                      <div key={level} className="flex items-center space-x-2">
                        <Checkbox
                          id={`physical-${level}`}
                          checked={filterForm.values.physical_energy.includes(
                            level,
                          )}
                          onCheckedChange={(checked) =>
                            handleMultiSelectFilter(
                              "physical_energy",
                              level,
                              checked as boolean,
                            )
                          }
                        />
                        <Label
                          htmlFor={`physical-${level}`}
                          className="text-sm capitalize"
                        >
                          {level}
                        </Label>
                      </div>
                    ))}
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
                            {activity.age_min}-{activity.age_max}
                          </p>
                        </div>
                        <div>
                          <span className="text-xs text-muted-foreground">
                            Duration
                          </span>
                          <p className="text-sm font-medium">
                            {activity.duration_min_minutes}-
                            {activity.duration_max_minutes} min
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
                                    {activity.mental_load}
                                  </span>
                                  <span className="inline-flex items-center gap-1 px-1 py-0.5 rounded-full text-xs font-medium bg-success/10 text-success border border-success/20">
                                    <ActivityIcon className="h-2 w-2" />
                                    {activity.physical_energy}
                                  </span>
                                </div>
                              </div>
                            </td>
                            <td className="px-2 py-3 whitespace-nowrap">
                              <div className="text-sm font-medium text-foreground">
                                {activity.age_min}-{activity.age_max}
                              </div>
                            </td>
                            <td className="px-2 py-3 whitespace-nowrap">
                              <span className="inline-flex items-center px-1.5 py-0.5 rounded-full text-xs font-medium bg-primary/10 text-primary border border-primary/20">
                                {activity.format}
                              </span>
                            </td>
                            <td className="px-2 py-3 whitespace-nowrap">
                              <div className="text-sm font-medium text-foreground">
                                {activity.duration_min_minutes}-
                                {activity.duration_max_minutes}m
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
