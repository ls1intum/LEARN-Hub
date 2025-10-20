import React, { useState, useEffect, useCallback } from "react";
import { BookOpen } from "lucide-react";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Skeleton } from "@/components/ui/skeleton";
import { LessonPlanCard } from "./LessonPlanCard";
import { apiService } from "@/services/apiService";
import { useAuth } from "@/hooks/useAuth";
import type { Activity } from "@/types/activity";

interface LessonPlanFavourite {
  id: number;
  favourite_type: string;
  name: string | null;
  activity_ids: number[];
  lesson_plan?: import("@/types/activity").LessonPlanData;
  created_at: string;
}

export const LessonPlanFavouritesTab: React.FC = () => {
  const { user } = useAuth();
  const [favourites, setFavourites] = useState<LessonPlanFavourite[]>([]);
  const [activitiesMap, setActivitiesMap] = useState<Map<number, Activity>>(
    new Map(),
  );
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [removingIds, setRemovingIds] = useState<Set<number>>(new Set());

  const loadFavourites = useCallback(async () => {
    if (!user) return;

    try {
      setLoading(true);
      setError(null);

      const response = await apiService.getLessonPlanFavourites();
      setFavourites(response.favourites);

      // With enforced snapshots, activity fetching becomes a no-op, but keep fallback for safety
      const idsNeedingFetch = response.favourites
        .filter((fav) => !fav.lesson_plan)
        .flatMap((fav) => fav.activity_ids);
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

  const removeFavourite = async (favouriteId: number) => {
    if (!user) return;

    try {
      setRemovingIds((prev) => new Set(prev).add(favouriteId));
      await apiService.deleteFavourite(favouriteId);

      // Remove from local state
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

  useEffect(() => {
    loadFavourites();
  }, [loadFavourites]);

  if (loading) {
    return (
      <div className="space-y-6">
        {[...Array(3)].map((_, i) => (
          <div
            key={i}
            className="bg-card border border-border/50 rounded-xl p-6"
          >
            <div className="space-y-4">
              <div className="flex items-start justify-between">
                <div className="flex-1">
                  <Skeleton className="h-6 w-3/4 mb-2" />
                  <Skeleton className="h-4 w-1/2" />
                </div>
                <Skeleton className="h-8 w-8" />
              </div>
              <div className="flex gap-2">
                <Skeleton className="h-6 w-16" />
                <Skeleton className="h-6 w-20" />
                <Skeleton className="h-6 w-24" />
              </div>
              <div className="space-y-3">
                <Skeleton className="h-4 w-full" />
                <Skeleton className="h-4 w-3/4" />
              </div>
            </div>
          </div>
        ))}
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
          No favourite lesson plans yet
        </h3>
        <p className="text-muted-foreground">
          Start favouriting lesson plans from the recommendations to see them
          here.
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {favourites.map((favourite) => {
        const activities: Activity[] = favourite.lesson_plan
          ? favourite.lesson_plan.activities
          : ((favourite.activity_ids || [])
              .map((id) => activitiesMap.get(id))
              .filter(Boolean) as Activity[]);

        if (!activities || activities.length === 0) return null;

        const isRemoving = removingIds.has(favourite.id);

        return (
          <LessonPlanCard
            key={favourite.id}
            favourite={favourite}
            activities={activities}
            onRemove={removeFavourite}
            isRemoving={isRemoving}
          />
        );
      })}
    </div>
  );
};
