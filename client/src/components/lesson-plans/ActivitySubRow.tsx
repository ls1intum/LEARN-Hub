import React from "react";
import { Badge } from "@/components/ui/badge";
import { BreakCard } from "@/components/BreakCard";
import { useTranslateEnum } from "@/hooks/useTranslateEnum";
import type { Activity } from "@/types/activity";

interface ActivitySubRowProps {
  activity: Activity;
  index: number;
  onClick?: (activity: Activity) => void;
  /** Show description instead of source below the name */
  showDescription?: boolean;
}

export const ActivitySubRow: React.FC<ActivitySubRowProps> = ({
  activity,
  index,
  onClick,
  showDescription = false,
}) => {
  const translateEnum = useTranslateEnum();
  const subtitle = showDescription ? activity.description : activity.source;

  return (
    <>
      <div
        className="flex items-center gap-2 pl-9 pr-3 py-2 hover:bg-muted/20 transition-colors cursor-pointer"
        onClick={() => onClick?.(activity)}
      >
        {/* Step indicator */}
        <div className="w-5 h-5 rounded-full bg-primary/10 border border-primary/20 flex items-center justify-center shrink-0">
          <span className="text-[10px] font-semibold text-primary tabular-nums">
            {index + 1}
          </span>
        </div>

        {/* Name */}
        <div className="flex-1 min-w-0">
          <p className="text-sm truncate">{activity.name}</p>
          {subtitle && (
            <p className="text-xs text-muted-foreground truncate">{subtitle}</p>
          )}
        </div>

        {/* Age */}
        <span className="w-[52px] text-xs text-muted-foreground tabular-nums hidden sm:block shrink-0">
          {activity.ageMin}–{activity.ageMax}
        </span>

        {/* Duration */}
        <span className="w-[68px] text-xs text-muted-foreground tabular-nums hidden sm:block shrink-0">
          {activity.durationMinMinutes}
          {activity.durationMaxMinutes && `–${activity.durationMaxMinutes}`}m
        </span>

        {/* Format */}
        <div className="w-[76px] shrink-0">
          <Badge
            variant="outline"
            className="text-[11px] px-1.5 py-0 font-normal truncate max-w-full"
          >
            {translateEnum("format", activity.format)}
          </Badge>
        </div>

        {/* Topics */}
        <div className="w-[84px] hidden md:flex items-center gap-1 shrink-0 min-w-0">
          {activity.topics?.slice(0, 1).map((topic) => (
            <Badge
              key={topic}
              variant="outline"
              className="text-[11px] px-1.5 py-0 font-normal truncate max-w-full"
            >
              {translateEnum("topics", topic)}
            </Badge>
          ))}
          {activity.topics && activity.topics.length > 1 && (
            <span className="text-[11px] text-muted-foreground shrink-0">
              +{activity.topics.length - 1}
            </span>
          )}
        </div>
      </div>

      {/* Break after activity */}
      {activity.breakAfter && (
        <div className="pl-12 pr-3 py-1.5">
          <BreakCard
            breakItem={activity.breakAfter}
            compact={true}
            isBetweenActivities={true}
            activityIndex={index + 1}
          />
        </div>
      )}
    </>
  );
};
