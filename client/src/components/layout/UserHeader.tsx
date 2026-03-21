import React from "react";
import { Button } from "@/components/ui/button";
import { ThemeToggle } from "@/components/ThemeToggle";
import { LanguageSwitcher } from "@/components/LanguageSwitcher";
import { LogOut, X } from "lucide-react";
import { useTranslation } from "react-i18next";

interface UserHeaderProps {
  user: {
    email?: string;
    role?: string;
  } | null;
  onLogout: () => void;
  onClose?: () => void;
  isMobile?: boolean;
  className?: string;
}

export const UserHeader: React.FC<UserHeaderProps> = ({
  user,
  onLogout,
  onClose,
  isMobile = false,
  className = "",
}) => {
  const { t } = useTranslation();
  const isAdmin = user?.role === "ADMIN";
  const isGuest = user?.role === "GUEST";

  return (
    <div className={`p-4 border-b border-border ${className}`}>
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3 min-w-0">
          <div className="w-8 h-8 rounded-lg bg-primary flex items-center justify-center flex-shrink-0">
            <span className="text-primary-foreground font-bold text-sm">
              {isAdmin ? "A" : isGuest ? "G" : "T"}
            </span>
          </div>
          <div className="min-w-0">
            <h1 className="text-sm font-semibold text-foreground leading-tight">
              {isAdmin
                ? t("userHeader.adminPanel")
                : isGuest
                  ? t("userHeader.guestAccess")
                  : t("userHeader.teachingHub")}
            </h1>
            <p className="text-xs text-muted-foreground truncate leading-tight mt-0.5">
              {user?.email && !isGuest
                ? user.email
                : isGuest
                  ? t("userHeader.guestUser")
                  : t("userHeader.activityCenter")}
            </p>
          </div>
        </div>
        <div className="flex items-center gap-1 flex-shrink-0">
          <LanguageSwitcher />
          <ThemeToggle />
          {isMobile && onClose ? (
            <Button
              onClick={onClose}
              variant="ghost"
              size="icon"
              aria-label="Close navigation menu"
              className="h-8 w-8 hover:bg-muted transition-colors"
            >
              <X className="h-4 w-4" />
            </Button>
          ) : (
            <Button
              onClick={onLogout}
              variant="ghost"
              size="icon"
              aria-label="Logout"
              className="h-8 w-8 hover:bg-destructive/10 hover:text-destructive transition-colors"
            >
              <LogOut className="h-4 w-4" />
            </Button>
          )}
        </div>
      </div>
    </div>
  );
};
