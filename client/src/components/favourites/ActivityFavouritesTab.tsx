import React, { useState, useEffect, useCallback } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { Heart, Trash2, Calendar, Clock, Users, Eye } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Skeleton } from "@/components/ui/skeleton";
import { apiService } from "@/services/apiService";
import { useAuth } from "@/hooks/useAuth";
import type { Activity } from "@/types/activity";
import { useTranslation } from "react-i18next";
import { useRestoreScroll } from "@/hooks/useRestoreScroll";
import { getAppScrollTop } from "@/utils/scroll";

interface ActivityFavourite {
  id: string;
  favouriteType: string;
  activityId: string;
  name: string | null;
  createdAt: string;
}

export const ActivityFavouritesTab: React.FC = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const { t } = useTranslation();

  const translateEnum = (category: string, value: string): string => {
    const key = `enums.${category}.${value}`;
    const translated = t(key);
    return translated === key ? value : translated;
  };
  const [favourites, setFavourites] = useState<ActivityFavourite[]>([]);
  const [activities, setActivities] = useState<Activity[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [removingIds, setRemovingIds] = useState<Set<string>>(new Set());

  const loadFavourites = useCallback(async () => {
    if (!user) return;

    try {
      setLoading(true);
      setError(null);

      const response = await apiService.getActivityFavourites();
      setFavourites(response.favourites);

      // Load activity details for each favourite
      const activityIds = response.favourites.map((fav) => fav.activityId);
      if (activityIds.length > 0) {
        const activityDetails =
          await apiService.getActivitiesByIds(activityIds);
        setActivities(activityDetails);
      }
    } catch (err) {
      setError(
        err instanceof Error ? err.message : "Failed to load favourites",
      );
    } finally {
      setLoading(false);
    }
  }, [user]);

  const removeFavourite = async (activityId: string) => {
    if (!user) return;

    try {
      setRemovingIds((prev) => new Set(prev).add(activityId));
      await apiService.removeActivityFavourite(activityId);

      // Remove from local state
      setFavourites((prev) =>
        prev.filter((fav) => fav.activityId !== activityId),
      );
      setActivities((prev) =>
        prev.filter((activity) => activity.id !== activityId),
      );
    } catch (err) {
      setError(
        err instanceof Error ? err.message : "Failed to remove favourite",
      );
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

  if (loading) {
    return (
      <div className="space-y-4">
        {[...Array(3)].map((_, i) => (
          <Card key={i}>
            <CardHeader>
              <Skeleton className="h-6 w-3/4" />
              <Skeleton className="h-4 w-1/2" />
            </CardHeader>
            <CardContent>
              <div className="flex gap-2 mb-4">
                <Skeleton className="h-6 w-16" />
                <Skeleton className="h-6 w-20" />
                <Skeleton className="h-6 w-24" />
              </div>
              <Skeleton className="h-4 w-full" />
            </CardContent>
          </Card>
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
        <Heart className="mx-auto h-12 w-12 text-muted-foreground mb-4" />
        <h3 className="text-lg font-semibold text-foreground mb-2">
          {t("activityFavourites.noFavourites")}
        </h3>
        <p className="text-muted-foreground">
          {t("activityFavourites.noFavouritesDesc")}
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {favourites.map((favourite) => {
        const activity = activities.find((a) => a.id === favourite.activityId);
        if (!activity) return null;

        const isRemoving = removingIds.has(activity.id);

        return (
          <Card
            key={favourite.id}
            className="hover:shadow-md transition-shadow"
          >
            <CardHeader>
              <div className="flex items-start justify-between">
                <div className="flex-1">
                  <CardTitle className="text-lg">{activity.name}</CardTitle>
                  <p className="text-sm text-muted-foreground mt-1">
                    {activity.description}
                  </p>
                </div>
                <div className="flex gap-2">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => handleViewDetails(activity)}
                    className="flex items-center gap-1"
                  >
                    <Eye className="h-4 w-4" />
                    {t("activityFavourites.viewDetails")}
                  </Button>
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => removeFavourite(activity.id)}
                    disabled={isRemoving}
                    className="text-destructive hover:text-destructive"
                  >
                    <Trash2 className="h-4 w-4" />
                  </Button>
                </div>
              </div>
            </CardHeader>
            <CardContent>
              <div className="flex flex-wrap gap-2 mb-4">
                <Badge variant="secondary">
                  <Users className="h-3 w-3 mr-1" />
                  {t("activityFavourites.ages")} {activity.ageMin}-
                  {activity.ageMax}
                </Badge>
                <Badge variant="secondary">
                  <Clock className="h-3 w-3 mr-1" />
                  {activity.durationMinMinutes}
                  {activity.durationMaxMinutes &&
                    `-${activity.durationMaxMinutes}`}{" "}
                  min
                </Badge>
                <Badge variant="secondary">
                  {translateEnum("format", activity.format)}
                </Badge>
                <Badge variant="outline">
                  {translateEnum("bloomLevel", activity.bloomLevel)}
                </Badge>
              </div>

              <div className="flex flex-wrap gap-1 mb-4">
                {activity.topics.map((topic) => (
                  <Badge key={topic} variant="outline" className="text-xs">
                    {translateEnum("topics", topic)}
                  </Badge>
                ))}
              </div>

              <div className="flex items-center justify-between text-sm text-muted-foreground">
                <div className="flex items-center">
                  <Calendar className="h-4 w-4 mr-1" />
                  {t("activityFavourites.favourited")}{" "}
                  {new Date(favourite.createdAt).toLocaleDateString()}
                </div>
                {favourite.name && (
                  <div className="text-right">
                    <span className="font-medium">
                      {t("activityFavourites.customName")}
                    </span>{" "}
                    {favourite.name}
                  </div>
                )}
              </div>
            </CardContent>
          </Card>
        );
      })}
    </div>
  );
};
