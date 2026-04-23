import React from "react";
import { Clock, Coffee, Users, Sparkles } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { LessonPlanFavouriteButton } from "@/components/favourites/LessonPlanFavouriteButton";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import { useTranslateEnum } from "@/hooks/useTranslateEnum";
import { useTranslation } from "react-i18next";
import type { Recommendation, Activity } from "@/types/activity";

const getScoreColor = (score: number) => {
  if (score >= 80) return "bg-green-500";
  if (score >= 60) return "bg-yellow-500";
  if (score >= 40) return "bg-orange-500";
  return "bg-red-500";
};

interface RecommendationCardProps {
  recommendation: Recommendation;
  totalDuration: number;
  activityCount: number;
  breakCount: number;
  minAge: number;
  maxAge: number;
  formats: string[];
  onSelect: () => void;
  onViewActivity: (activity: Activity) => void;
}

export const RecommendationCard: React.FC<RecommendationCardProps> = ({
  recommendation,
  totalDuration,
  activityCount,
  breakCount,
  minAge,
  maxAge,
  formats,
  onSelect,
  onViewActivity,
}) => {
  const { t } = useTranslation();
  const translateEnum = useTranslateEnum();
  const { score, scoreBreakdown, activities } = recommendation;
  const MAX_VISIBLE = 4;

  return (
    <div className="border border-border rounded-lg p-3.5 bg-card flex flex-col gap-3 h-full">
      {/* Score + meta row */}
      <div className="flex items-center gap-2 flex-wrap">
        <TooltipProvider>
          <Tooltip>
            <TooltipTrigger asChild>
              <div className="flex items-center gap-1.5 cursor-pointer">
                <div
                  className={`w-2 h-2 rounded-full shrink-0 ${getScoreColor(score)}`}
                />
                <span className="text-sm font-semibold tabular-nums">
                  {Math.round(score)}%
                </span>
              </div>
            </TooltipTrigger>
            <TooltipContent side="bottom" className="max-w-xs">
              <div className="space-y-2">
                <div className="font-semibold text-xs">
                  {t("resultsDisplay.categoryScores")}
                </div>
                <div className="space-y-1 text-xs">
                  {Object.entries(scoreBreakdown || {}).map(
                    ([category, scoreData]) => (
                      <div
                        key={category}
                        className="flex justify-between gap-4"
                      >
                        <span className="capitalize">
                          {category.replace(/_/g, " ")}
                        </span>
                        <span className="font-medium tabular-nums">
                          {scoreData.score}%
                          {scoreData.isPriority && (
                            <span className="ml-1 text-yellow-500">★</span>
                          )}
                        </span>
                      </div>
                    ),
                  )}
                </div>
              </div>
            </TooltipContent>
          </Tooltip>
        </TooltipProvider>

        <div className="flex items-center gap-1.5 text-xs text-muted-foreground ml-auto">
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
        </div>
      </div>

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
      <div className="flex items-center gap-1.5 pt-2 border-t border-border/50">
        <Badge
          variant="secondary"
          className="text-[11px] px-1.5 py-0 font-normal"
        >
          {translateEnum("format", formats[0])}
        </Badge>
        <div className="ml-auto flex items-center gap-1.5">
          <LessonPlanFavouriteButton
            activities={activities}
            size="icon"
            className="h-7 w-7 shrink-0"
          />
          <Button size="sm" onClick={onSelect} className="h-7 px-2 text-xs">
            <Sparkles className="h-3 w-3 mr-1" />
            {t("resultsDisplay.select")}
          </Button>
        </div>
      </div>
    </div>
  );
};

export const RecommendationCardSkeleton: React.FC = () => (
  <div className="border border-border rounded-lg p-3.5 bg-card flex flex-col gap-3 h-full min-h-[176px]">
    <div className="flex items-center gap-2">
      <Skeleton className="h-2 w-2 rounded-full" />
      <Skeleton className="h-4 w-10" />
      <div className="ml-auto flex items-center gap-1.5">
        <Skeleton className="h-3 w-10" />
        <Skeleton className="h-3 w-10" />
        <Skeleton className="h-3 w-6" />
      </div>
    </div>

    <div className="flex flex-col gap-2 flex-1">
      {Array.from({ length: 4 }).map((_, i) => (
        <div key={i} className="flex items-center gap-2">
          <Skeleton className="h-4 w-4 rounded-full" />
          <Skeleton className="h-3 flex-1" />
        </div>
      ))}
    </div>

    <div className="flex items-center gap-1.5 pt-2 border-t border-border/50">
      <Skeleton className="h-5 w-16 rounded-full" />
      <div className="ml-auto flex items-center gap-1.5">
        <Skeleton className="h-7 w-7 rounded" />
        <Skeleton className="h-7 w-20 rounded" />
      </div>
    </div>
  </div>
);
