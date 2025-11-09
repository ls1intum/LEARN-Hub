import React from "react";
import { useNavigate } from "react-router-dom";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Clock, Users } from "lucide-react";
import { FavouriteButton } from "@/components/favourites/FavouriteButton";
import type { Activity } from "@/types/activity";

interface ActivityCardProps {
  activity: Activity;
  compact?: boolean;
  className?: string;
  isFavourited?: boolean;
}

export const ActivityCard: React.FC<ActivityCardProps> = ({
  activity,
  compact = false,
  className = "",
  isFavourited,
}) => {
  const navigate = useNavigate();

  const ageRange =
    activity.age_min && activity.age_max
      ? `${activity.age_min}-${activity.age_max}`
      : activity.age_min
        ? `${activity.age_min}+`
        : "";

  const durationRange =
    activity.duration_min_minutes && activity.duration_max_minutes
      ? `${activity.duration_min_minutes}-${activity.duration_max_minutes} min`
      : activity.duration_min_minutes
        ? `${activity.duration_min_minutes}+ min`
        : "";

  const handleViewDetails = (e: React.MouseEvent) => {
    e.stopPropagation();
    if (
      activity.id &&
      activity.type === "activity" &&
      typeof activity.id === "number"
    ) {
      navigate(`/activity-details/${activity.id}`, {
        state: { activity },
      });
    }
  };

  return (
    <Card
      className={`group transition-all duration-200 hover:shadow-lg hover:-translate-y-0.5 border border-border/50 bg-card/50 backdrop-blur-sm ${className}`}
    >
      <CardHeader className={compact ? "pb-3" : "pb-4"}>
        <div className="flex items-start justify-between gap-2">
          <CardTitle
            className={`${compact ? "text-sm" : "text-base"} font-semibold leading-tight text-card-foreground group-hover:text-primary transition-colors line-clamp-2 flex-1`}
          >
            {activity.name}
          </CardTitle>
          <FavouriteButton
            activityId={activity.id}
            initialIsFavourited={isFavourited}
          />
        </div>
      </CardHeader>

      <CardContent className="pt-0 space-y-3">
        {/* Description */}
        {activity.description && (
          <p className="text-sm text-muted-foreground line-clamp-2 leading-relaxed">
            {activity.description}
          </p>
        )}

        {/* Key Info - Compact Layout */}
        <div className="flex items-center gap-4 text-xs text-muted-foreground">
          {ageRange && (
            <div className="flex items-center gap-1">
              <Users className="h-3 w-3" />
              <span className="font-medium text-card-foreground">
                {ageRange}
              </span>
            </div>
          )}
          {durationRange && (
            <div className="flex items-center gap-1">
              <Clock className="h-3 w-3" />
              <span className="font-medium text-card-foreground">
                {durationRange}
              </span>
            </div>
          )}
        </div>

        {/* Format and Bloom Level - Cleaner Badges */}
        <div className="flex flex-wrap gap-1.5">
          {activity.format && (
            <Badge
              variant="outline"
              className="text-xs px-2 py-0.5 bg-primary/5 text-primary border-primary/20 hover:bg-primary/10 transition-colors"
            >
              {activity.format}
            </Badge>
          )}
          {activity.bloom_level && (
            <Badge
              variant="secondary"
              className="text-xs px-2 py-0.5 bg-secondary/50 text-secondary-foreground"
            >
              {activity.bloom_level}
            </Badge>
          )}
        </div>

        {/* Energy Levels - Minimal Design */}
        {(activity.mental_load || activity.physical_energy) && (
          <div className="flex gap-4 text-xs">
            {activity.mental_load && (
              <div className="flex items-center gap-1.5">
                <div className="w-2 h-2 rounded-full bg-blue-500" />
                <span className="text-muted-foreground">Mental:</span>
                <span className="font-medium text-card-foreground capitalize">
                  {activity.mental_load}
                </span>
              </div>
            )}
            {activity.physical_energy && (
              <div className="flex items-center gap-1.5">
                <div className="w-2 h-2 rounded-full bg-orange-500" />
                <span className="text-muted-foreground">Physical:</span>
                <span className="font-medium text-card-foreground capitalize">
                  {activity.physical_energy}
                </span>
              </div>
            )}
          </div>
        )}

        {/* Topics - Cleaner Display */}
        {activity.topics && activity.topics.length > 0 && (
          <div className="flex flex-wrap gap-1">
            {activity.topics.slice(0, 2).map((topic, index) => (
              <Badge
                key={index}
                variant="outline"
                className="text-xs px-2 py-0.5 bg-muted/50 text-muted-foreground border-muted-foreground/20"
              >
                {topic}
              </Badge>
            ))}
            {activity.topics.length > 2 && (
              <Badge
                variant="outline"
                className="text-xs px-2 py-0.5 bg-muted/50 text-muted-foreground border-muted-foreground/20"
              >
                +{activity.topics.length - 2}
              </Badge>
            )}
          </div>
        )}

        {/* View Details Button - Always show */}
        {activity.id && activity.type === "activity" && (
          <Button
            variant="outline"
            size="sm"
            onClick={handleViewDetails}
            className={`${compact ? "mt-1" : "w-full"} h-8 text-xs hover:bg-primary/5 hover:border-primary/20 transition-colors`}
          >
            View Details
          </Button>
        )}
      </CardContent>
    </Card>
  );
};
