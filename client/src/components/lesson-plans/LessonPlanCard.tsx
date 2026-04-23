import React from "react";
import { Clock, Coffee, Users, Trash2 } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { useTranslateEnum } from "@/hooks/useTranslateEnum";
import { useTranslation } from "react-i18next";
import type { Activity } from "@/types/activity";

interface LessonPlanCardProps {
  name: string | null;
  activities: Activity[];
  totalDuration: number;
  minAge: number;
  maxAge: number;
  activityCount: number;
  breakCount: number;
  formats: string[];
  createdAt: string;
  onRemove: () => void;
  isRemoving: boolean;
  onViewActivity: (activity: Activity) => void;
}

const MAX_VISIBLE = 4;

export const LessonPlanCard: React.FC<LessonPlanCardProps> = ({
  name,
  activities,
  totalDuration,
  minAge,
  maxAge,
  activityCount,
  breakCount,
  formats,
  createdAt,
  onRemove,
  isRemoving,
  onViewActivity,
}) => {
  const { t } = useTranslation();
  const translateEnum = useTranslateEnum();

  return (
    <div className="border border-border rounded-lg p-3.5 bg-card flex flex-col gap-2.5 h-full">
      {/* Label row */}
      <div className="flex items-center justify-between gap-2">
        <span className="text-[10px] font-semibold text-muted-foreground uppercase tracking-wider">
          {t("lessonPlanFavourites.lessonPlanLabel", {
            count: activityCount,
            defaultValue: `Lesson Plan · ${activityCount} activities`,
          })}
        </span>
        <span className="text-[11px] text-muted-foreground tabular-nums shrink-0">
          {new Date(createdAt).toLocaleDateString()}
        </span>
      </div>

      {/* Title */}
      <h3 className="text-sm font-semibold text-foreground leading-snug line-clamp-2">
        {name || t("lessonPlan.untitled")}
      </h3>

      {/* Activity list */}
      <div className="flex flex-col gap-1 flex-1">
        {activities.slice(0, MAX_VISIBLE).map((activity, idx) => (
          <button
            key={activity.id || idx}
            type="button"
            className="flex items-center gap-2 text-left group hover:bg-muted/40 rounded px-1.5 py-1 transition-colors -mx-1.5"
            onClick={() => onViewActivity(activity)}
          >
            <div className="w-4 h-4 rounded-full bg-primary/10 border border-primary/20 flex items-center justify-center shrink-0">
              <span className="text-[9px] font-semibold text-primary tabular-nums">
                {idx + 1}
              </span>
            </div>
            <span className="text-xs text-foreground truncate group-hover:text-primary transition-colors">
              {activity.name}
            </span>
          </button>
        ))}
        {activityCount > MAX_VISIBLE && (
          <p className="text-[11px] text-muted-foreground pl-1.5">
            +{activityCount - MAX_VISIBLE} more
          </p>
        )}
      </div>

      {/* Footer */}
      <div className="flex items-center gap-2 pt-1.5 border-t border-border/50">
        <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
          <span className="flex items-center gap-0.5">
            <Clock className="h-3 w-3 shrink-0" />
            <span className="tabular-nums">{totalDuration}m</span>
          </span>
          <span className="flex items-center gap-0.5">
            <Users className="h-3 w-3 shrink-0" />
            {minAge}–{maxAge}
          </span>
          {breakCount > 0 && (
            <span className="flex items-center gap-0.5 text-blue-500">
              <Coffee className="h-3 w-3" />
              {breakCount}
            </span>
          )}
          <Badge
            variant="secondary"
            className="text-[11px] px-1.5 py-0 font-normal"
          >
            {translateEnum("format", formats[0])}
          </Badge>
        </div>
        <div className="ml-auto shrink-0">
          <Button
            variant="ghost"
            size="icon"
            className="h-7 w-7 text-destructive"
            onClick={onRemove}
            disabled={isRemoving}
            aria-label="Remove favourite"
          >
            <Trash2 className="h-3.5 w-3.5" />
          </Button>
        </div>
      </div>
    </div>
  );
};

export const LessonPlanCardSkeleton: React.FC = () => (
  <div className="border border-border rounded-lg p-3.5 bg-card flex flex-col gap-2.5 h-full min-h-[188px]">
    <div className="flex items-center justify-between gap-2">
      <Skeleton className="h-3 w-32" />
      <Skeleton className="h-3 w-16" />
    </div>

    <Skeleton className="h-4 w-4/5" />

    <div className="flex flex-col gap-2 flex-1">
      {Array.from({ length: 4 }).map((_, i) => (
        <div key={i} className="flex items-center gap-2">
          <Skeleton className="h-4 w-4 rounded-full" />
          <Skeleton className="h-3 flex-1" />
        </div>
      ))}
    </div>

    <div className="flex items-center gap-2 pt-1.5 border-t border-border/50">
      <Skeleton className="h-3 w-10" />
      <Skeleton className="h-3 w-10" />
      <Skeleton className="h-5 w-16 rounded-full" />
      <Skeleton className="h-7 w-7 rounded ml-auto" />
    </div>
  </div>
);
