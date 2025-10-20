import React from "react";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Clock, Users, BookOpen } from "lucide-react";
import type { Activity } from "@/types/activity";

interface TimelineItemProps {
  activity: Activity;
  stepNumber: number;
}

export const TimelineItem: React.FC<TimelineItemProps> = ({
  activity,
  stepNumber,
}) => {
  const formatDuration = (minutes: number) => {
    if (minutes < 60) {
      return `${minutes}m`;
    }
    const hours = Math.floor(minutes / 60);
    const mins = minutes % 60;
    return mins > 0 ? `${hours}h ${mins}m` : `${hours}h`;
  };

  const formatAgeRange = (min: number, max: number) => {
    return min === max ? `${min} years` : `${min}-${max} years`;
  };

  return (
    <Card className="relative">
      <CardContent className="p-4">
        <div className="flex items-start gap-4">
          {/* Step Number */}
          <div className="flex-shrink-0 w-8 h-8 bg-primary text-primary-foreground rounded-full flex items-center justify-center text-sm font-semibold">
            {stepNumber}
          </div>

          {/* Activity Content */}
          <div className="flex-1 space-y-3">
            {/* Title and Description */}
            <div>
              <h4 className="text-lg font-semibold text-foreground">
                {activity.name}
              </h4>
              {activity.description && (
                <p className="text-muted-foreground mt-1">
                  {activity.description}
                </p>
              )}
            </div>

            {/* Activity Details */}
            <div className="flex flex-wrap gap-2 text-sm">
              {/* Duration */}
              <div className="flex items-center gap-1 text-muted-foreground">
                <Clock className="h-4 w-4" />
                <span>{formatDuration(activity.duration_min_minutes)}</span>
              </div>

              {/* Age Range */}
              <div className="flex items-center gap-1 text-muted-foreground">
                <Users className="h-4 w-4" />
                <span>
                  {formatAgeRange(activity.age_min, activity.age_max)}
                </span>
              </div>

              {/* Format */}
              <Badge variant="secondary" className="text-xs">
                {activity.format}
              </Badge>

              {/* Bloom Level */}
              <Badge variant="outline" className="text-xs">
                {activity.bloom_level}
              </Badge>
            </div>

            {/* Topics */}
            {activity.topics && activity.topics.length > 0 && (
              <div className="flex items-center gap-2">
                <BookOpen className="h-4 w-4 text-muted-foreground" />
                <div className="flex flex-wrap gap-1">
                  {activity.topics.map((topic, index) => (
                    <Badge key={index} variant="outline" className="text-xs">
                      {topic}
                    </Badge>
                  ))}
                </div>
              </div>
            )}

            {/* Resources */}
            {activity.resources_needed &&
              activity.resources_needed.length > 0 && (
                <div className="text-sm">
                  <span className="text-muted-foreground">Resources: </span>
                  <span className="text-foreground">
                    {activity.resources_needed.join(", ")}
                  </span>
                </div>
              )}
          </div>
        </div>
      </CardContent>
    </Card>
  );
};
