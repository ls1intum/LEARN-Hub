import React from "react";
import { Clock, Users, Trash2 } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { FavouriteButton } from "@/components/favourites/FavouriteButton";
import { Skeleton } from "@/components/ui/skeleton";
import { useTranslateEnum } from "@/hooks/useTranslateEnum";
import type { Activity } from "@/types/activity";

const BLOOM_ORDER = [
  "remember",
  "understand",
  "apply",
  "analyze",
  "evaluate",
  "create",
];

const BLOOM_COLORS: Record<string, string> = {
  remember: "bg-slate-400",
  understand: "bg-blue-400",
  apply: "bg-green-500",
  analyze: "bg-yellow-400",
  evaluate: "bg-orange-400",
  create: "bg-purple-500",
};

interface ActivityCardProps {
  activity: Activity;
  onClick: () => void;
  showRemoveButton?: boolean;
  isRemoving?: boolean;
  onRemove?: () => void;
}

export const ActivityCard: React.FC<ActivityCardProps> = ({
  activity,
  onClick,
  showRemoveButton,
  isRemoving,
  onRemove,
}) => {
  const translateEnum = useTranslateEnum();
  const bloomKey = activity.bloomLevel?.toLowerCase() ?? "";
  const bloomIndex = BLOOM_ORDER.indexOf(bloomKey);
  const bloomColor = BLOOM_COLORS[bloomKey] ?? "bg-primary";

  return (
    <div
      className="border border-border rounded-lg p-3.5 hover:shadow-md hover:border-border/60 transition-all cursor-pointer bg-card flex flex-col gap-2.5 h-full"
      onClick={onClick}
    >
      {/* Title + action */}
      <div className="flex items-start justify-between gap-2">
        <div className="min-w-0 flex-1">
          <h3 className="text-sm font-semibold text-foreground leading-snug line-clamp-2">
            {activity.name}
          </h3>
          {activity.source && (
            <p className="text-xs text-muted-foreground truncate mt-0.5">
              {activity.source}
            </p>
          )}
        </div>
        <div className="shrink-0" onClick={(e) => e.stopPropagation()}>
          {showRemoveButton ? (
            <Button
              variant="ghost"
              size="icon"
              className="h-7 w-7 text-destructive"
              onClick={(e) => {
                e.stopPropagation();
                onRemove?.();
              }}
              disabled={isRemoving}
            >
              <Trash2 className="h-3.5 w-3.5" />
            </Button>
          ) : (
            <FavouriteButton
              activityId={activity.id}
              variant="ghost"
              size="icon"
              className="h-7 w-7"
              initialIsFavourited={activity.isFavourited ?? false}
            />
          )}
        </div>
      </div>

      {/* Description */}
      {activity.description && (
        <p className="text-xs text-muted-foreground line-clamp-2 leading-relaxed">
          {activity.description}
        </p>
      )}

      {/* Topics */}
      {activity.topics && activity.topics.length > 0 && (
        <div className="flex flex-wrap gap-1">
          {activity.topics.slice(0, 3).map((topic) => (
            <Badge
              key={topic}
              variant="secondary"
              className="text-[11px] px-1.5 py-0 font-normal"
            >
              {translateEnum("topics", topic)}
            </Badge>
          ))}
          {activity.topics.length > 3 && (
            <span className="text-[11px] text-muted-foreground self-center">
              +{activity.topics.length - 3}
            </span>
          )}
        </div>
      )}

      <div className="flex-1" />

      {/* Bloom level indicator */}
      {bloomIndex >= 0 && (
        <div className="flex items-center gap-1.5">
          <div className="flex items-center gap-0.5">
            {BLOOM_ORDER.map((_, i) => (
              <div
                key={i}
                className={`w-2 h-2 rounded-full transition-colors ${i <= bloomIndex ? bloomColor : "bg-muted"}`}
              />
            ))}
          </div>
          <span className="text-[11px] text-muted-foreground">
            {translateEnum("bloomLevel", activity.bloomLevel)}
          </span>
        </div>
      )}

      {/* Footer */}
      <div className="flex items-center gap-2 text-xs text-muted-foreground pt-1.5 border-t border-border/50">
        <span className="flex items-center gap-0.5">
          <Users className="h-3 w-3 shrink-0" />
          {activity.ageMin}–{activity.ageMax}
        </span>
        <span className="flex items-center gap-0.5">
          <Clock className="h-3 w-3 shrink-0" />
          {activity.durationMinMinutes}
          {activity.durationMaxMinutes ? `–${activity.durationMaxMinutes}` : ""}
          m
        </span>
        <div className="ml-auto">
          <Badge
            variant="secondary"
            className="text-[11px] px-1.5 py-0 font-normal"
          >
            {translateEnum("format", activity.format)}
          </Badge>
        </div>
      </div>
    </div>
  );
};

export const ActivityCardSkeleton: React.FC = () => (
  <div className="border border-border rounded-lg p-3.5 bg-card flex flex-col gap-2.5 h-full min-h-[188px]">
    <div className="flex items-start justify-between gap-2">
      <div className="min-w-0 flex-1 space-y-2">
        <Skeleton className="h-4 w-5/6" />
        <Skeleton className="h-3 w-1/2" />
      </div>
      <Skeleton className="h-7 w-7 rounded" />
    </div>

    <div className="space-y-1.5">
      <Skeleton className="h-3 w-full" />
      <Skeleton className="h-3 w-2/3" />
    </div>

    <div className="flex flex-wrap gap-1">
      <Skeleton className="h-5 w-16 rounded-full" />
      <Skeleton className="h-5 w-20 rounded-full" />
      <Skeleton className="h-5 w-14 rounded-full" />
    </div>

    <div className="flex-1" />

    <div className="flex items-center gap-1.5">
      <div className="flex items-center gap-0.5">
        {Array.from({ length: 6 }).map((_, i) => (
          <Skeleton key={i} className="h-2 w-2 rounded-full" />
        ))}
      </div>
      <Skeleton className="h-3 w-20" />
    </div>

    <div className="flex items-center gap-2 pt-1.5 border-t border-border/50">
      <Skeleton className="h-3 w-10" />
      <Skeleton className="h-3 w-14" />
      <Skeleton className="h-5 w-16 rounded-full ml-auto" />
    </div>
  </div>
);
