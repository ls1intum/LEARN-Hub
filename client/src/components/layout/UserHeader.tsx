import React from "react";
import { X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useTranslation } from "react-i18next";

interface UserHeaderProps {
  user: {
    email?: string;
    role?: string;
  } | null;
  onClose?: () => void;
  isMobile?: boolean;
  className?: string;
}

export const UserHeader: React.FC<UserHeaderProps> = ({
  user,
  onClose,
  isMobile = false,
  className = "",
}) => {
  const { t } = useTranslation();
  const isAdmin = user?.role === "ADMIN";
  const isGuest = user?.role === "GUEST";

  const roleDescription = isAdmin
    ? t("sidebar.adminView")
    : isGuest
      ? t("sidebar.guestView")
      : t("sidebar.teacherView");

  return (
    <div className={`p-4 border-b border-border ${className}`}>
      <div className="flex items-center justify-between">
        <div className="min-w-0">
          <h1 className="text-base font-bold text-foreground leading-tight tracking-tight">
            LEARN-Hub
          </h1>
          <p className="text-xs text-muted-foreground leading-tight mt-0.5">
            {roleDescription}
          </p>
        </div>
        {isMobile && onClose && (
          <Button
            onClick={onClose}
            variant="ghost"
            size="icon"
            aria-label="Close navigation menu"
            className="h-8 w-8 hover:bg-muted transition-colors flex-shrink-0"
          >
            <X className="h-4 w-4" />
          </Button>
        )}
      </div>
    </div>
  );
};
