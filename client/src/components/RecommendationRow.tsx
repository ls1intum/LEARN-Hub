import React, { useState, useRef, useEffect } from "react";
import { Button } from "@/components/ui/button";
import { ActivityCard } from "@/components/ActivityCard";
import { BreakCard } from "@/components/BreakCard";
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import { LessonPlanFavouriteButton } from "@/components/favourites/LessonPlanFavouriteButton";
import { Clock, Star, Users, Coffee, Sparkles } from "lucide-react";
import type { Recommendation } from "@/types/activity";

interface RecommendationRowProps {
  recommendation: Recommendation;
  index: number;
  onSelect: () => void;
}

export const RecommendationRow: React.FC<RecommendationRowProps> = ({
  recommendation,
  onSelect,
}) => {
  const { activities, score, score_breakdown } = recommendation;

  // Tooltip state management
  const [isTooltipOpen, setIsTooltipOpen] = useState(false);
  const [isHovering, setIsHovering] = useState(false);
  const [isClicked, setIsClicked] = useState(false);
  const tooltipRef = useRef<HTMLDivElement>(null);

  // Extract activities and their embedded breaks
  const activityItems = activities;
  const breakItems = activities
    .filter((activity) => activity.break_after)
    .map((activity) => activity.break_after!);

  // Get score color based on value
  const getScoreColor = (score: number) => {
    if (score >= 80) return "bg-green-500";
    if (score >= 60) return "bg-yellow-500";
    if (score >= 40) return "bg-orange-500";
    return "bg-red-500";
  };

  // Use actual category scores from API response
  const categoryScores = score_breakdown || {};

  // Handle tooltip visibility logic
  useEffect(() => {
    setIsTooltipOpen(isHovering || isClicked);
  }, [isHovering, isClicked]);

  // Handle outside click to close tooltip
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (
        tooltipRef.current &&
        !tooltipRef.current.contains(event.target as Node)
      ) {
        setIsClicked(false);
        setIsTooltipOpen(false);
      }
    };

    if (isClicked) {
      document.addEventListener("mousedown", handleClickOutside);
    }

    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, [isClicked]);

  // Event handlers
  const handleMouseEnter = () => {
    setIsHovering(true);
  };

  const handleMouseLeave = () => {
    setIsHovering(false);
  };

  const handleClick = (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setIsClicked(!isClicked);
  };

  return (
    <div className="group bg-card border border-border/50 rounded-xl p-6 hover:shadow-lg hover:border-border transition-all duration-300 hover:-translate-y-0.5">
      {/* Header with Score and Actions */}
      <div className="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-4 mb-6">
        <div className="flex flex-col sm:flex-row sm:items-center gap-4">
          {/* Score Badge */}
          <TooltipProvider>
            <Tooltip open={isTooltipOpen} onOpenChange={setIsTooltipOpen}>
              <TooltipTrigger asChild>
                <div
                  ref={tooltipRef}
                  className="flex items-center gap-2 cursor-pointer"
                  onMouseEnter={handleMouseEnter}
                  onMouseLeave={handleMouseLeave}
                  onClick={handleClick}
                >
                  <div
                    className={`w-3 h-3 rounded-full ${getScoreColor(score)}`}
                  />
                  <span className="text-sm font-semibold text-foreground">
                    {Math.round(score)}%
                  </span>
                </div>
              </TooltipTrigger>
              <TooltipContent side="bottom" className="max-w-xs">
                <div className="space-y-2">
                  <div className="font-semibold">Category Scores:</div>
                  <div className="space-y-1 text-sm">
                    {Object.entries(categoryScores).map(
                      ([category, scoreData]) => (
                        <div key={category} className="flex justify-between">
                          <span className="capitalize">
                            {category.replace("_", " ")}:
                          </span>
                          <span className="font-medium">
                            {scoreData.score}%
                            {scoreData.is_priority && (
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

          <div className="hidden sm:block h-4 w-px bg-border" />

          <div className="flex flex-wrap items-center gap-3 sm:gap-4 text-sm text-muted-foreground">
            <div className="flex items-center gap-1.5">
              <Clock className="h-4 w-4" />
              <span className="font-medium">
                {activities.reduce((total, activity) => {
                  const activityDuration = activity.duration_min_minutes || 0;
                  const breakDuration = activity.break_after?.duration || 0;
                  return total + activityDuration + breakDuration;
                }, 0)}{" "}
                min
              </span>
            </div>
            <div className="flex items-center gap-1.5">
              <Users className="h-4 w-4" />
              <span>
                {activityItems.length} activit
                {activityItems.length !== 1 ? "ies" : "y"}
              </span>
            </div>
            {breakItems.length > 0 && (
              <div className="flex items-center gap-1.5">
                <Coffee className="h-4 w-4" />
                <span>
                  {breakItems.length} break
                  {breakItems.length !== 1 ? "s" : ""}
                </span>
              </div>
            )}
          </div>
        </div>

        <div className="flex gap-2 sm:flex-shrink-0">
          <LessonPlanFavouriteButton activities={activities} />
          <Button
            size="sm"
            onClick={onSelect}
            className="h-8 px-3 text-xs flex-1 sm:flex-none"
          >
            <Sparkles className="h-3 w-3 mr-1" />
            Select
          </Button>
        </div>
      </div>

      {/* Timeline View - Activities and Breaks in Sequence */}
      <div className="space-y-4">
        <div className="flex items-center gap-2 mb-3">
          <Star className="h-4 w-4 text-primary" />
          <h4 className="text-sm font-semibold text-foreground">
            Lesson Timeline
          </h4>
        </div>

        {/* Timeline Container */}
        <div className="relative">
          {/* Timeline Line */}
          <div className="absolute left-4 top-0 bottom-0 w-0.5 bg-gradient-to-b from-primary/30 via-primary/50 to-primary/30"></div>

          {/* Timeline Items */}
          <div className="space-y-3">
            {activityItems.map((activity, activityIndex) => (
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
                    <ActivityCard activity={activity} compact={true} />
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
      </div>
    </div>
  );
};
