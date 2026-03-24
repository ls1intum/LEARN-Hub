import React from "react";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Clock, Users, BookOpen } from "lucide-react";
import type { Activity } from "@/types/activity";
import { useTranslation } from "react-i18next";

interface TimelineItemProps {
  activity: Activity;
  stepNumber: number;
}

export const TimelineItem: React.FC<TimelineItemProps> = ({
  activity,
  stepNumber,
}) => {
  const { t } = useTranslation();

  const translateEnum = (category: string, value: string): string => {
    const key = `enums.${category}.${value}`;
    const translated = t(key);
    return translated === key ? value : translated;
  };
  const formatDuration = (minutes: number) => {
    if (minutes < 60) {
      return `${minutes}m`;
    }
    const hours = Math.floor(minutes / 60);
    const mins = minutes % 60;
    return mins > 0 ? `${hours}h ${mins}m` : `${hours}h`;
  };

  const formatAgeRange = (min: number, max: number) => {
    return min === max ? `${min} ${t("common.years")}` : `${min}-${max} ${t("common.years")}`;
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
                <span>{formatDuration(activity.durationMinMinutes)}</span>
              </div>

              {/* Age Range */}
              <div className="flex items-center gap-1 text-muted-foreground">
                <Users className="h-4 w-4" />
                <span>{formatAgeRange(activity.ageMin, activity.ageMax)}</span>
              </div>

              {/* Format */}
              <Badge variant="secondary" className="text-xs">
                {translateEnum("format", activity.format)}
              </Badge>

              {/* Bloom Level */}
              <Badge variant="outline" className="text-xs">
                {translateEnum("bloomLevel", activity.bloomLevel)}
              </Badge>
            </div>

            {/* Topics */}
            {activity.topics && activity.topics.length > 0 && (
              <div className="flex items-center gap-2">
                <BookOpen className="h-4 w-4 text-muted-foreground" />
                <div className="flex flex-wrap gap-1">
                  {activity.topics.map((topic, index) => (
                    <Badge key={index} variant="outline" className="text-xs">
                      {translateEnum("topics", topic)}
                    </Badge>
                  ))}
                </div>
              </div>
            )}

            {/* Resources */}
            {activity.resourcesNeeded &&
              activity.resourcesNeeded.length > 0 && (
                <div className="text-sm">
                  <span className="text-muted-foreground">{t("timeline.resources")} </span>
                  <span className="text-foreground">
                    {activity.resourcesNeeded.map((r) => translateEnum("resources", r)).join(", ")}
                  </span>
                </div>
              )}
          </div>
        </div>
      </CardContent>
    </Card>
  );
};
