import React from "react";
import { Badge } from "@/components/ui/badge";
import { Clock, Coffee } from "lucide-react";
import type { BreakAfter } from "@/types/activity";

interface BreakCardProps {
  breakItem: BreakAfter;
  compact?: boolean;
  isBetweenActivities?: boolean;
  activityIndex?: number;
}

export const BreakCard: React.FC<BreakCardProps> = ({
  breakItem,
  compact = false,
  isBetweenActivities = false,
  activityIndex,
}) => {
  const { duration, description, reasons = [] } = breakItem;

  return (
    <div
      className={`${
        isBetweenActivities
          ? "bg-gradient-to-br from-blue-50/80 to-amber-50/60 dark:from-blue-900/20 dark:to-amber-900/10 border-2 border-dashed border-blue-300/60 dark:border-blue-700/60"
          : "bg-blue-50/50 dark:bg-blue-900/10 border border-blue-200/50 dark:border-blue-800/50"
      } rounded-lg p-3 group hover:bg-blue-50/70 dark:hover:bg-blue-900/20 transition-colors ${compact ? "p-2" : "p-3"}`}
    >
      <div className="flex items-start gap-3">
        <div className="flex-shrink-0">
          <div
            className={`${
              isBetweenActivities
                ? "w-7 h-7 bg-gradient-to-br from-blue-100 to-amber-100 dark:from-blue-900/30 dark:to-amber-900/30"
                : "w-6 h-6 bg-blue-100/80 dark:bg-blue-900/20"
            } rounded-full flex items-center justify-center group-hover:bg-blue-100 dark:group-hover:bg-blue-900/30 transition-colors`}
          >
            <Coffee
              className={`${isBetweenActivities ? "h-4 w-4" : "h-3 w-3"} text-blue-600 dark:text-blue-400`}
            />
          </div>
        </div>
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 mb-1">
            <h4
              className={`font-semibold text-blue-900 dark:text-blue-100 ${compact ? "text-sm" : "text-base"}`}
            >
              {isBetweenActivities
                ? `Break After Activity ${activityIndex}`
                : "Break"}
            </h4>
            <Badge
              variant="outline"
              className={`text-xs px-2 py-0.5 ${
                isBetweenActivities
                  ? "border-blue-400/60 text-blue-800 dark:text-blue-200 bg-blue-200/60 dark:bg-blue-800/30"
                  : "border-blue-300/50 text-blue-700 dark:text-blue-300 bg-blue-100/50 dark:bg-blue-900/20"
              }`}
            >
              <Clock className="h-3 w-3 mr-1" />
              {duration} min
            </Badge>
          </div>

          {isBetweenActivities && (
            <div className="text-xs text-blue-600/70 dark:text-blue-400/70 mb-2 font-medium">
              Transition between activities
            </div>
          )}

          {description && (
            <p
              className={`text-blue-700/80 dark:text-blue-300/80 mb-2 ${compact ? "text-xs" : "text-sm"} line-clamp-2`}
            >
              {description}
            </p>
          )}

          {reasons.length > 0 && (
            <div className="flex items-center gap-2">
              <Badge
                variant="secondary"
                className="text-xs px-2 py-0.5 bg-blue-100/70 dark:bg-blue-900/30 text-blue-800 dark:text-blue-200"
              >
                {reasons.length} reason
                {reasons.length !== 1 ? "s" : ""}
              </Badge>
            </div>
          )}

          {reasons.length > 0 && !compact && (
            <div className="mt-3 pt-2 border-t border-blue-200/50 dark:border-blue-800/50">
              <div className="text-xs font-medium text-blue-600 dark:text-blue-400 mb-1">
                Reasons:
              </div>
              <ul className="text-xs text-blue-700/80 dark:text-blue-300/80 space-y-1">
                {reasons.slice(0, 2).map((reason, index) => (
                  <li key={index} className="flex items-start gap-1.5">
                    <span className="text-blue-500 dark:text-blue-400 mt-0.5">
                      •
                    </span>
                    <span className="line-clamp-1">{reason}</span>
                  </li>
                ))}
                {reasons.length > 2 && (
                  <li className="text-blue-600/70 dark:text-blue-400/70 text-xs">
                    +{reasons.length - 2} more
                  </li>
                )}
              </ul>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
