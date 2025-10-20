import React from "react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Clock, Users, Coffee, Eye } from "lucide-react";
import { ActivityCard } from "@/components/ActivityCard";
import { BreakCard } from "@/components/BreakCard";
import type { Activity } from "@/types/activity";

interface LessonPlanRowProps {
  activities: Activity[];
  totalDuration?: number;
  onSelect?: () => void;
  onViewDetails?: (activities: Activity[]) => void;
  className?: string;
  score?: number;
}

export const LessonPlanRow: React.FC<LessonPlanRowProps> = ({
  activities,
  totalDuration,
  onSelect,
  onViewDetails,
  className = "",
  score,
}) => {
  const getActivityDuration = (activity: Activity, isLast: boolean) => {
    // Include the break duration only if the activity is not the last one
    let duration = activity.duration_min_minutes || 0;
    if (!isLast && activity.break_after) {
      duration += activity.break_after.duration;
    }
    return duration;
  };

  const totalDurationCalculated =
    totalDuration ||
    activities.reduce((acc, activity, index) => {
      const isLast = index === activities.length - 1;
      return acc + getActivityDuration(activity, isLast);
    }, 0);

  const activityCount = activities.length;
  const breakCount = activities.filter((a, index) => index < activities.length - 1 && a.break_after).length;

  // Calculate card widths based on content and available space
  const getCardWidth = (activity: Activity) => {
    const baseWidth = 280; // Base width for activity cards
    const breakWidth = 200; // Smaller width for break cards

    if (activity.break_after) {
      return breakWidth;
    }

    // Adjust width based on name length and content
    const nameLength = activity.name.length;
    if (nameLength > 50) return baseWidth + 40;
    if (nameLength > 30) return baseWidth + 20;
    return baseWidth;
  };

  return (
    <div className={`space-y-4 ${className}`}>
      {/* Header with summary and actions */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-6">
          <div className="flex items-center gap-2">
            <Clock className="h-4 w-4 text-muted-foreground" />
            <span className="text-sm font-medium">
              {totalDurationCalculated} min
            </span>
          </div>
          <div className="flex items-center gap-2">
            <Users className="h-4 w-4 text-muted-foreground" />
            <span className="text-sm font-medium">
              {activityCount} activit{activityCount !== 1 ? "ies" : "y"}
            </span>
          </div>
          {breakCount > 0 && (
            <div className="flex items-center gap-2">
              <Coffee className="h-4 w-4 text-muted-foreground" />
              <span className="text-sm font-medium">
                {breakCount} break{breakCount !== 1 ? "s" : ""}
              </span>
            </div>
          )}
          {score !== undefined && (
            <Badge
              variant="secondary"
              className="bg-success/10 text-success border-success/20"
            >
              Score: {score.toFixed(2)}
            </Badge>
          )}
        </div>

        <div className="flex items-center gap-2">
          {onViewDetails && (
            <Button
              onClick={() => onViewDetails(activities)}
              variant="outline"
              size="sm"
              className="flex items-center gap-1"
            >
              <Eye className="h-4 w-4" />
              View Details
            </Button>
          )}
          {onSelect && (
            <Button
              onClick={onSelect}
              size="sm"
              className="bg-gradient-to-r from-primary to-primary/90 hover:from-primary/90 hover:to-primary"
            >
              Select Plan
            </Button>
          )}
        </div>
      </div>

      {/* Activity Cards Row */}
      <div className="flex gap-4 overflow-x-auto pb-2">
        {activities.map((activity, index) => (
          <React.Fragment key={`activity-${activity.id || index}`}>
            <div
              className="flex-shrink-0"
              style={{ width: `${getCardWidth(activity)}px` }}
            >
              <ActivityCard
                activity={activity}
                className="h-full"
                compact={true}
              />
            </div>
            {activity.break_after && index < activities.length - 1 && (
              <>
                {/* Visual connector line */}
                <div className="flex-shrink-0 flex items-center justify-center px-2">
                  <div className="w-8 h-0.5 bg-gradient-to-r from-blue-300 to-blue-400 dark:from-blue-600 dark:to-blue-500 relative">
                    <div className="absolute -top-1 -right-1 w-2 h-2 bg-blue-400 dark:bg-blue-500 rounded-full"></div>
                  </div>
                </div>
                {/* Break card with between-activities styling */}
                <div
                  className="flex-shrink-0"
                  style={{ width: `${getCardWidth(activity)}px` }}
                >
                  <BreakCard
                    breakItem={activity.break_after}
                    isBetweenActivities={true}
                    activityIndex={index + 1}
                  />
                </div>
                {/* Visual connector line after break */}
                <div className="flex-shrink-0 flex items-center justify-center px-2">
                  <div className="w-8 h-0.5 bg-gradient-to-r from-blue-400 to-blue-300 dark:from-blue-500 dark:to-blue-600 relative">
                    <div className="absolute -top-1 -left-1 w-2 h-2 bg-blue-400 dark:bg-blue-500 rounded-full"></div>
                  </div>
                </div>
              </>
            )}
          </React.Fragment>
        ))}
      </div>
    </div>
  );
};
