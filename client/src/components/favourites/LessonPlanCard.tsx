import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { ActivityCard } from "@/components/ActivityCard";
import { Badge } from "@/components/ui/badge";
import {
  Trash2,
  Calendar,
  Clock,
  Users,
  BookOpen,
  Star,
  Eye,
  Coffee,
} from "lucide-react";
import type { Activity } from "@/types/activity";
import { BreakCard } from "@/components/BreakCard";

interface LessonPlanFavourite {
  id: number;
  favourite_type: string;
  name: string | null;
  activity_ids: number[];
  lesson_plan?: import("@/types/activity").LessonPlanData;
  created_at: string;
}

interface LessonPlanCardProps {
  favourite: LessonPlanFavourite;
  activities: Activity[];
  onRemove: (favouriteId: number) => void;
  isRemoving: boolean;
}

export const LessonPlanCard: React.FC<LessonPlanCardProps> = ({
  favourite,
  activities,
  onRemove,
  isRemoving,
}) => {
  const navigate = useNavigate();
  const [showAllActivities, setShowAllActivities] = useState(false);

  // Calculate lesson plan summary (including breaks)
  const totalDuration = activities.reduce((sum, activity) => {
    const activityMinutes = activity.duration_min_minutes || 0;
    const breakMinutes = activity.break_after?.duration || 0;
    return sum + activityMinutes + breakMinutes;
  }, 0);
  const allTopics = [
    ...new Set(activities.flatMap((activity) => activity.topics)),
  ];
  const allFormats = [
    ...new Set(activities.map((activity) => activity.format)),
  ];
  const minAge = Math.min(...activities.map((activity) => activity.age_min));
  const maxAge = Math.max(...activities.map((activity) => activity.age_max));

  const handleViewActivityDetails = (activity: Activity) => {
    if (
      activity.id &&
      activity.type === "activity" &&
      typeof activity.id === "number"
    ) {
      navigate(`/activity-details/${activity.id}`, {
        state: { activity, fromBrowser: true },
      });
    }
  };

  const displayedActivities = showAllActivities
    ? activities
    : activities.slice(0, 3);
  const hasMoreActivities = activities.length > 3;

  return (
    <div className="group bg-card border border-border/50 rounded-xl p-6 hover:shadow-lg hover:border-border transition-all duration-300 hover:-translate-y-0.5">
      {/* Header with Title and Actions */}
      <div className="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-4 mb-6">
        <div className="flex-1">
          <div className="flex items-start justify-between gap-4">
            <div className="flex-1">
              <h3 className="text-xl font-bold text-foreground mb-2">
                {favourite.name || "Untitled Lesson Plan"}
              </h3>
              <p className="text-sm text-muted-foreground">
                {activities.length} activities • {totalDuration} minutes total
              </p>
            </div>
            <Button
              variant="ghost"
              size="sm"
              onClick={() => onRemove(favourite.id)}
              disabled={isRemoving}
              className="text-destructive hover:text-destructive flex-shrink-0"
            >
              <Trash2 className="h-4 w-4" />
            </Button>
          </div>
        </div>
      </div>

      {/* Summary Information */}
      <div className="flex flex-wrap items-center gap-3 sm:gap-4 text-sm text-muted-foreground mb-6">
        <div className="flex items-center gap-1.5">
          <Users className="h-4 w-4" />
          <span className="font-medium">
            Ages {minAge}-{maxAge}
          </span>
        </div>
        <div className="flex items-center gap-1.5">
          <Clock className="h-4 w-4" />
          <span className="font-medium">{totalDuration} min</span>
        </div>
        <div className="flex items-center gap-1.5">
          <BookOpen className="h-4 w-4" />
          <span>{activities.length} activities</span>
        </div>
        <div className="flex items-center gap-1.5">
          <Calendar className="h-4 w-4" />
          <span>
            Favourited {new Date(favourite.created_at).toLocaleDateString()}
          </span>
        </div>
      </div>

      {/* Topics and Formats */}
      <div className="mb-6">
        <div className="flex flex-wrap gap-1.5 mb-3">
          {allTopics.slice(0, 5).map((topic) => (
            <Badge key={topic} variant="outline" className="text-xs">
              {topic}
            </Badge>
          ))}
          {allTopics.length > 5 && (
            <Badge variant="outline" className="text-xs">
              +{allTopics.length - 5} more
            </Badge>
          )}
        </div>
        <div className="flex flex-wrap gap-1.5">
          {allFormats.map((format) => (
            <Badge key={format} variant="secondary" className="text-xs">
              {format}
            </Badge>
          ))}
        </div>
      </div>

      {/* Activities Timeline (with inline breaks) */}
      <div className="space-y-4">
        <div className="flex items-center gap-2 mb-3">
          <Star className="h-4 w-4 text-primary" />
          <h4 className="text-sm font-semibold text-foreground">
            Activities ({activities.length})
          </h4>
        </div>

        {/* Timeline Container */}
        <div className="relative">
          {/* Timeline Line */}
          <div className="absolute left-4 top-0 bottom-0 w-0.5 bg-gradient-to-b from-primary/30 via-primary/50 to-primary/30"></div>

          {/* Timeline Items */}
          <div className="space-y-3">
            {displayedActivities.map((activity, activityIndex) => (
              <React.Fragment
                key={`timeline-${activityIndex}-${activity.id || "no-id"}`}
              >
                {/* Activity Item */}
                <div className="relative flex items-start gap-3">
                  {/* Timeline Dot */}
                  <div className="relative z-10 flex-shrink-0 w-8 h-8 bg-primary/10 rounded-full flex items-center justify-center border-2 border-primary/20">
                    <div className="w-2 h-2 bg-primary rounded-full"></div>
                  </div>

                  {/* Activity Content */}
                  <div className="flex-1 min-w-0">
                    <div className="text-xs text-muted-foreground mb-1 font-medium">
                      Step {activityIndex + 1}
                    </div>
                    <div className="relative">
                      <ActivityCard activity={activity} compact={true} />
                      {/* View Details Button */}
                      <div className="absolute top-2 right-2 flex gap-1">
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => handleViewActivityDetails(activity)}
                          className="h-7 px-2 text-xs bg-background/95 backdrop-blur-sm border-border/50 hover:bg-accent"
                        >
                          <Eye className="h-3 w-3 mr-1" />
                          View
                        </Button>
                      </div>
                    </div>
                  </div>
                </div>

                {/* Break After Activity */}
                {activity.break_after && (
                  <div className="relative flex items-start gap-3 ml-4">
                    {/* Break Timeline Dot */}
                    <div className="relative z-10 flex-shrink-0 w-6 h-6 bg-blue-100 dark:bg-blue-900/30 rounded-full flex items-center justify-center border border-blue-300 dark:border-blue-700">
                      <Coffee className="h-2.5 w-2.5 text-blue-600 dark:text-blue-400" />
                    </div>

                    {/* Break Content */}
                    <div className="flex-1 min-w-0">
                      <div className="text-xs text-blue-600/70 dark:text-blue-400/70 mb-1 font-medium">
                        Break after Step {activityIndex + 1}
                      </div>
                      <BreakCard
                        breakItem={activity.break_after}
                        compact={true}
                        isBetweenActivities={true}
                        activityIndex={activityIndex + 1}
                      />
                    </div>
                  </div>
                )}
              </React.Fragment>
            ))}
          </div>
        </div>

        {/* Show More/Less Button */}
        {hasMoreActivities && (
          <div className="flex justify-center pt-4">
            <Button
              variant="outline"
              size="sm"
              onClick={() => setShowAllActivities(!showAllActivities)}
              className="h-8 px-4 text-xs"
            >
              {showAllActivities
                ? "Show Less"
                : `Show All ${activities.length} Activities`}
            </Button>
          </div>
        )}
      </div>
    </div>
  );
};
