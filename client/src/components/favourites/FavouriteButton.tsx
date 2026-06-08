import React, { useState, useEffect } from "react";
import { Heart } from "lucide-react";
import { Button } from "@/components/ui/button";
import { apiService } from "@/services/apiService";
import { useAuth } from "@/hooks/useAuth";
import { logger } from "@/services/logger";
import { useNavigate, useLocation } from "react-router-dom";
import { useTranslation } from "react-i18next";
import type { AuthRedirectState } from "@/utils/authRedirect";
import { cn } from "@/lib/utils";

interface FavouriteButtonProps {
  activityId: string;
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
  initialIsFavourited?: boolean;
}

export const FavouriteButton: React.FC<FavouriteButtonProps> = ({
  activityId,
  variant = "ghost",
  size = "sm",
  className = "",
  onToggle,
  initialIsFavourited,
}) => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const { t } = useTranslation();
  const [isFavourited, setIsFavourited] = useState(
    initialIsFavourited ?? false,
  );
  const [loading, setLoading] = useState(false);

  const toggleFavourite = async () => {
    if (!user || loading) return;

    try {
      setLoading(true);

      if (isFavourited) {
        await apiService.removeActivityFavourite(activityId);
        setIsFavourited(false);
        onToggle?.(false);
      } else {
        await apiService.saveActivityFavourite({ activityId: activityId });
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
    setIsFavourited(initialIsFavourited ?? false);
  }, [initialIsFavourited]);

  // Show unauthenticated users the button, but redirect to login on click
  if (!user) {
    return (
      <Button
        variant={variant}
        size={size}
        onClick={() => {
          navigate("/login", {
            state: {
              from: {
                pathname: location.pathname,
                search: location.search,
                hash: location.hash,
              },
              message: t("favourites.loginRequired"),
            } satisfies AuthRedirectState,
          });
        }}
        className={cn("text-muted-foreground hover:text-red-500", className)}
        title={t("favourites.loginRequired")}
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
      className={cn(
        "text-muted-foreground hover:text-red-500",
        className,
        isFavourited && "text-red-500 hover:text-red-600",
      )}
    >
      <Heart className={`h-4 w-4 ${isFavourited ? "fill-current" : ""}`} />
    </Button>
  );
};
