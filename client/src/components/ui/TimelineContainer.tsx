import React from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Clock, Users, BookOpen, ExternalLink } from "lucide-react";
import type { Activity } from "@/types/activity";
import { useTranslation } from "react-i18next";

interface TimelineContainerProps {
  children: React.ReactNode;
  className?: string;
}

export const TimelineContainer: React.FC<TimelineContainerProps> = ({
  children,
  className = "",
}) => {
  return (
    <div className={`relative ${className}`}>
      {/* Timeline Line */}
      <div className="absolute left-6 top-0 bottom-0 w-0.5 bg-gradient-to-b from-primary/30 via-primary/50 to-primary/30"></div>

      {/* Timeline Items */}
      <div className="space-y-4">{children}</div>
    </div>
  );
};

interface TimelineItemProps {
  activity: Activity;
  stepNumber: number;
  className?: string;
}

export const TimelineItem: React.FC<TimelineItemProps> = ({
  activity,
  stepNumber,
  className = "",
}) => {
  const { t } = useTranslation();

  const translateEnum = (category: string, value: string): string => {
    const key = `enums.${category}.${value}`;
    const translated = t(key);
    return translated === key ? value : translated;
  };

  const getAgeRange = (activity: Activity) => {
    if (activity.ageMin && activity.ageMax) {
      return `${activity.ageMin}-${activity.ageMax}`;
    }
    return t("timeline.allAges");
  };

  return (
    <div className={`relative flex items-start gap-4 ${className}`}>
      {/* Timeline Dot */}
      <div className="relative z-10 flex-shrink-0 w-12 h-12 bg-primary/10 rounded-full flex items-center justify-center border-2 border-primary/20">
        <div className="w-3 h-3 bg-primary rounded-full"></div>
      </div>

      {/* Activity Content */}
      <div className="flex-1 min-w-0">
        <div className="text-sm text-muted-foreground mb-2 font-medium">
          {t("timeline.step", { number: stepNumber })}
        </div>
        <Card className="hover:shadow-md transition-shadow">
          <CardHeader>
            <div className="flex items-start justify-between">
              <div className="flex-1">
                <CardTitle className="text-lg mb-2">{activity.name}</CardTitle>
                {activity.description && (
                  <p className="text-sm text-muted-foreground mb-3 leading-relaxed">
                    {activity.description}
                  </p>
                )}
                <p className="text-sm text-muted-foreground mb-3">
                  {activity.source}
                </p>
              </div>
              <Badge variant="outline">{translateEnum("format", activity.format)}</Badge>
            </div>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-4">
              <div className="flex items-center text-sm text-muted-foreground">
                <Users className="h-4 w-4 mr-2" />
                {t("timeline.ages")} {getAgeRange(activity)}
              </div>
              <div className="flex items-center text-sm text-muted-foreground">
                <Clock className="h-4 w-4 mr-2" />
                {activity.durationMinMinutes}
                {activity.durationMaxMinutes &&
                activity.durationMaxMinutes !== activity.durationMinMinutes
                  ? `-${activity.durationMaxMinutes}`
                  : ""}{" "}
                {t("timeline.minutes")}
              </div>
              <div className="flex items-center text-sm text-muted-foreground">
                <BookOpen className="h-4 w-4 mr-2" />
                {translateEnum("bloomLevel", activity.bloomLevel)}
              </div>
            </div>

            {/* Topics */}
            {activity.topics && activity.topics.length > 0 && (
              <div className="mb-4">
                <div className="flex flex-wrap gap-1">
                  {activity.topics.map((topic, topicIndex) => (
                    <Badge
                      key={topicIndex}
                      variant="secondary"
                      className="text-xs"
                    >
                      {translateEnum("topics", topic)}
                    </Badge>
                  ))}
                </div>
              </div>
            )}

            {/* Resources */}
            {activity.resourcesNeeded &&
              activity.resourcesNeeded.length > 0 && (
                <div className="mb-4">
                  <p className="text-sm font-medium text-foreground mb-2">
                    {t("timeline.resourcesNeeded")}
                  </p>
                  <div className="flex flex-wrap gap-1">
                    {activity.resourcesNeeded.map((resource, resourceIndex) => (
                      <Badge
                        key={resourceIndex}
                        variant="outline"
                        className="text-xs"
                      >
                        {translateEnum("resources", resource)}
                      </Badge>
                    ))}
                  </div>
                </div>
              )}

            {/* Activity Details Link */}
            {activity.id && (
              <div className="pt-2 border-t">
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => {
                    // Open activity details in new tab
                    window.open(`/activity-details/${activity.id}`, "_blank");
                  }}
                >
                  <ExternalLink className="h-3 w-3 mr-1" />
                  {t("timeline.viewFullDetails")}
                </Button>
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
};
