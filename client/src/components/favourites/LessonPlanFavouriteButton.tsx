import React, { useState } from "react";
import { Heart } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { apiService } from "@/services/apiService";
import { useAuth } from "@/hooks/useAuth";
import type { Activity } from "@/types/activity";
import { logger } from "@/services/logger";

interface LessonPlanFavouriteButtonProps {
  activities: Activity[];
  variant?:
    | "default"
    | "ghost"
    | "outline"
    | "secondary"
    | "destructive"
    | "link";
  size?: "default" | "sm" | "lg" | "icon";
  className?: string;
  onSave?: () => void;
}

export const LessonPlanFavouriteButton: React.FC<
  LessonPlanFavouriteButtonProps
> = ({
  activities,
  variant = "ghost",
  size = "sm",
  className = "",
  onSave,
}) => {
  const { user } = useAuth();
  const [open, setOpen] = useState(false);
  const [name, setName] = useState("");
  const [loading, setLoading] = useState(false);

  const handleSave = async () => {
    if (!user || loading || activities.length === 0) return;

    try {
      setLoading(true);

      const activityIds = activities.map((activity) => activity.id);
      // Include a minimal snapshot so favourites can render breaks and metadata consistently
      const totalDuration = activities.reduce(
        (sum, a) => sum + (a.duration_min_minutes || 0) + (a.break_after?.duration || 0),
        0,
      );
      await apiService.saveLessonPlanFavourite({
        activity_ids: activityIds,
        name: name.trim() || undefined,
        lesson_plan: {
          activities,
          total_duration_minutes: totalDuration,
          ordering_strategy: "balanced",
          title: name.trim() || "My Lesson Plan",
        },
      });

      setOpen(false);
      setName("");
      onSave?.();
    } catch (err) {
      logger.error(
        "Failed to save lesson plan favourite",
        err,
        "LessonPlanFavouriteButton",
      );
    } finally {
      setLoading(false);
    }
  };

  // Don't render if user is not logged in
  if (!user) {
    return null;
  }

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button
          variant={variant}
          size={size}
          className={`text-muted-foreground hover:text-red-500 ${className}`}
        >
          <Heart className="h-4 w-4" />
        </Button>
      </DialogTrigger>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Save Lesson Plan to Favourites</DialogTitle>
        </DialogHeader>
        <div className="space-y-4">
          <div>
            <Label htmlFor="name">Name (optional)</Label>
            <Input
              id="name"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="Enter a custom name for this lesson plan"
              className="mt-1"
            />
          </div>

          <div className="text-sm text-muted-foreground">
            <p>This lesson plan contains {activities.length} activities:</p>
            <ul className="mt-2 space-y-1">
              {activities.slice(0, 3).map((activity) => (
                <li key={activity.id} className="truncate">
                  • {activity.name}
                </li>
              ))}
              {activities.length > 3 && (
                <li className="text-muted-foreground">
                  • and {activities.length - 3} more activities
                </li>
              )}
            </ul>
          </div>

          <div className="flex justify-end space-x-2">
            <Button
              variant="outline"
              onClick={() => setOpen(false)}
              disabled={loading}
            >
              Cancel
            </Button>
            <Button
              onClick={handleSave}
              disabled={loading}
              className="bg-red-500 hover:bg-red-600"
            >
              {loading ? "Saving..." : "Save to Favourites"}
            </Button>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
};
