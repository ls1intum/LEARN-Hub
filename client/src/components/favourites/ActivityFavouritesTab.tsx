import React, { useState, useEffect, useCallback, useMemo } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { Heart, Trash2, Clock, Users, Search } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Skeleton } from "@/components/ui/skeleton";
import { Input } from "@/components/ui/input";
import { apiService } from "@/services/apiService";
import { useAuth } from "@/hooks/useAuth";
import type { Activity } from "@/types/activity";
import { useTranslation } from "react-i18next";
import { useTranslateEnum } from "@/hooks/useTranslateEnum";
import { useRestoreScroll } from "@/hooks/useRestoreScroll";
import { getAppScrollTop } from "@/utils/scroll";
import { PaginationBar } from "@/components/ui/PaginationBar";

interface ActivityFavourite {
  id: string;
  favouriteType: string;
  activityId: string;
  name: string | null;
  createdAt: string;
}

const ITEMS_PER_PAGE = 20;

export const ActivityFavouritesTab: React.FC = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const { t } = useTranslation();
  const translateEnum = useTranslateEnum();

  const [favourites, setFavourites] = useState<ActivityFavourite[]>([]);
  const [activities, setActivities] = useState<Activity[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [removingIds, setRemovingIds] = useState<Set<string>>(new Set());
  const [search, setSearch] = useState("");
  const [currentPage, setCurrentPage] = useState(1);

  const loadFavourites = useCallback(async () => {
    if (!user) return;

    try {
      setLoading(true);
      setError(null);

      const response = await apiService.getActivityFavourites();
      setFavourites(response.favourites);

      const activityIds = response.favourites.map((fav) => fav.activityId);
      if (activityIds.length > 0) {
        const activityDetails = await apiService.getActivitiesByIds(activityIds);
        setActivities(activityDetails);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load favourites");
    } finally {
      setLoading(false);
    }
  }, [user]);

  const removeFavourite = async (activityId: string) => {
    if (!user) return;

    try {
      setRemovingIds((prev) => new Set(prev).add(activityId));
      await apiService.removeActivityFavourite(activityId);
      setFavourites((prev) => prev.filter((fav) => fav.activityId !== activityId));
      setActivities((prev) => prev.filter((activity) => activity.id !== activityId));
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to remove favourite");
    } finally {
      setRemovingIds((prev) => {
        const newSet = new Set(prev);
        newSet.delete(activityId);
        return newSet;
      });
    }
  };

  const handleViewDetails = (activity: Activity) => {
    navigate(`/activity-details/${activity.id}`, {
      state: {
        activity,
        backTo: `${location.pathname}${location.search}`,
        restoreScrollY: getAppScrollTop(),
      },
    });
  };

  useEffect(() => {
    loadFavourites();
  }, [loadFavourites]);

  useRestoreScroll(!loading);

  // Client-side filtering
  const filteredRows = useMemo(() => {
    const q = search.trim().toLowerCase();
    return favourites
      .map((fav) => ({
        favourite: fav,
        activity: activities.find((a) => a.id === fav.activityId),
      }))
      .filter(({ activity }) => {
        if (!activity) return false;
        if (!q) return true;
        return activity.name.toLowerCase().includes(q);
      });
  }, [favourites, activities, search]);

  // Reset to page 1 when search changes
  useEffect(() => {
    setCurrentPage(1);
  }, [search]);

  const totalPages = Math.ceil(filteredRows.length / ITEMS_PER_PAGE);
  const pagedRows = filteredRows.slice(
    (currentPage - 1) * ITEMS_PER_PAGE,
    currentPage * ITEMS_PER_PAGE,
  );

  if (loading) {
    return (
      <div className="border border-border rounded-lg overflow-hidden">
        <div className="flex items-center gap-3 px-3 py-2 bg-muted/40 border-b border-border">
          <Skeleton className="h-4 w-32" />
        </div>
        <div className="divide-y divide-border">
          {[...Array(6)].map((_, i) => (
            <div key={i} className="flex items-center gap-3 px-3 py-2.5">
              <Skeleton className="h-4 flex-1" />
              <Skeleton className="h-5 w-12 hidden sm:block" />
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
        <Heart className="mx-auto h-10 w-10 text-muted-foreground mb-3" />
        <h3 className="text-sm font-semibold text-foreground mb-1">
          {t("activityFavourites.noFavourites")}
        </h3>
        <p className="text-xs text-muted-foreground">
          {t("activityFavourites.noFavouritesDesc")}
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {/* Search */}
      <div className="relative w-full">
        <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-muted-foreground pointer-events-none" />
        <Input
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder={t("activityFavourites.searchPlaceholder")}
          className="pl-8 h-8 text-sm w-full"
        />
      </div>
      <p className="text-xs text-muted-foreground tabular-nums">
        {filteredRows.length} {t("library.totalActivities")}
      </p>

      <div className="border border-border rounded-lg overflow-hidden">
        {/* Column header */}
        <div className="flex items-center gap-3 px-3 py-2 bg-muted/40 border-b border-border text-xs font-medium text-muted-foreground uppercase tracking-wide">
          <span className="flex-1">{t("activityFavourites.nameHeader")}</span>
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

        {pagedRows.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-10 text-center">
            <p className="text-sm text-muted-foreground">
              {t("activityFavourites.noResults")}
            </p>
          </div>
        ) : (
          <div className="divide-y divide-border">
            {pagedRows.map(({ favourite, activity }) => {
              if (!activity) return null;
              const isRemoving = removingIds.has(activity.id);

              return (
                <div
                  key={favourite.id}
                  className="flex items-center gap-3 px-3 py-2.5 hover:bg-muted/30 transition-colors cursor-pointer"
                  onClick={() => handleViewDetails(activity)}
                >
                  {/* Name */}
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium truncate">
                      {activity.name}
                    </p>
                    {favourite.name && (
                      <p className="text-xs text-muted-foreground truncate">
                        {favourite.name}
                      </p>
                    )}
                  </div>

                  {/* Age */}
                  <div className="w-[52px] shrink-0 hidden sm:flex items-center gap-0.5 text-xs text-muted-foreground">
                    <Users className="h-3 w-3 shrink-0" />
                    {activity.ageMin}–{activity.ageMax}
                  </div>

                  {/* Duration */}
                  <div className="w-[68px] shrink-0 hidden sm:flex items-center gap-0.5 text-xs text-muted-foreground">
                    <Clock className="h-3 w-3 shrink-0" />
                    {activity.durationMinMinutes}
                    {activity.durationMaxMinutes &&
                      `–${activity.durationMaxMinutes}`}
                    &nbsp;min
                  </div>

                  {/* Format badge */}
                  <div className="w-[76px] shrink-0">
                    <Badge variant="secondary" className="text-xs px-1.5 py-0 font-normal">
                      {translateEnum("format", activity.format)}
                    </Badge>
                  </div>

                  {/* Favourited date */}
                  <div className="w-[100px] shrink-0 hidden lg:block text-xs text-muted-foreground tabular-nums">
                    {new Date(favourite.createdAt).toLocaleDateString()}
                  </div>

                  {/* Actions */}
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
            })}
          </div>
        )}

        {pagedRows.length > 0 && (
          <div className="px-3">
            <PaginationBar
              currentPage={currentPage}
              totalPages={totalPages}
              totalItems={filteredRows.length}
              itemsPerPage={ITEMS_PER_PAGE}
              onPageChange={setCurrentPage}
            />
          </div>
        )}
      </div>
    </div>
  );
};
