import React, { useState, useEffect, useCallback } from "react";
import { Heart } from "lucide-react";
import { Button } from "@/components/ui/button";
import { apiService } from "@/services/apiService";
import { useAuth } from "@/hooks/useAuth";
import { logger } from "@/services/logger";

interface FavouriteButtonProps {
  activityId: number;
  variant?:
    | "default"
    | "ghost"
    | "outline"
    | "secondary"
    | "destructive"
    | "link";
  size?: "default" | "sm" | "lg" | "icon";
  className?: string;
  onToggle?: (isFavourited: boolean) => void;
}

export const FavouriteButton: React.FC<FavouriteButtonProps> = ({
  activityId,
  variant = "ghost",
  size = "sm",
  className = "",
  onToggle,
}) => {
  const { user } = useAuth();
  const [isFavourited, setIsFavourited] = useState(false);
  const [loading, setLoading] = useState(false);
  const [checkingStatus, setCheckingStatus] = useState(true);

  const checkFavouriteStatus = useCallback(async () => {
    if (!user) {
      setCheckingStatus(false);
      return;
    }

    try {
      const response =
        await apiService.checkActivityFavouriteStatus(activityId);
      setIsFavourited(response.is_favourited);
    } catch (err) {
      logger.error("Failed to check favourite status", err, "FavouriteButton");
    } finally {
      setCheckingStatus(false);
    }
  }, [user, activityId]);

  const toggleFavourite = async () => {
    if (!user || loading) return;

    try {
      setLoading(true);

      if (isFavourited) {
        await apiService.removeActivityFavourite(activityId);
        setIsFavourited(false);
        onToggle?.(false);
      } else {
        await apiService.saveActivityFavourite({ activity_id: activityId });
        setIsFavourited(true);
        onToggle?.(true);
      }
    } catch (err) {
      logger.error("Failed to toggle favourite", err, "FavouriteButton");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    checkFavouriteStatus();
  }, [checkFavouriteStatus]);

  // Don't render if user is not logged in
  if (!user) {
    return null;
  }

  // Don't render while checking status
  if (checkingStatus) {
    return (
      <Button
        variant={variant}
        size={size}
        className={`opacity-50 ${className}`}
        disabled
      >
        <Heart className="h-4 w-4" />
      </Button>
    );
  }

  return (
    <Button
      variant={variant}
      size={size}
      onClick={toggleFavourite}
      disabled={loading}
      className={`${isFavourited ? "text-red-500 hover:text-red-600" : "text-muted-foreground hover:text-red-500"} ${className}`}
    >
      <Heart className={`h-4 w-4 ${isFavourited ? "fill-current" : ""}`} />
    </Button>
  );
};
