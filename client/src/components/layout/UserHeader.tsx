import React from "react";
import { Button } from "@/components/ui/button";
import { ThemeToggle } from "@/components/ThemeToggle";
import { LogOut, X } from "lucide-react";

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
  const isAdmin = user?.role === "ADMIN";
  const isGuest = user?.role === "GUEST";

  return (
    <div className={`p-6 border-b border-border/50 ${className}`}>
      <div className="flex items-center justify-between">
        <div className="space-y-2">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-primary to-primary/80 flex items-center justify-center shadow-lg">
              <span className="text-primary-foreground font-bold text-lg">
                {isAdmin ? "A" : isGuest ? "G" : "T"}
              </span>
            </div>
            <div>
              <h1 className="text-xl font-bold text-foreground">
                {isAdmin
                  ? "Admin Panel"
                  : isGuest
                    ? "Guest Access"
                    : "Teaching Hub"}
              </h1>
              <p className="text-xs text-muted-foreground font-medium">
                {isAdmin
                  ? "System Management"
                  : isGuest
                    ? "Browse Activities"
                    : "Activity Center"}
              </p>
            </div>
          </div>
          {user?.email && !isGuest && (
            <div className="pl-13">
              <p className="text-sm text-muted-foreground bg-muted/30 rounded-lg px-3 py-1.5 inline-block">
                {user.email}
              </p>
            </div>
          )}
          {isGuest && (
            <div className="pl-13">
              <p className="text-sm text-muted-foreground bg-muted/30 rounded-lg px-3 py-1.5 inline-block">
                Guest User
              </p>
            </div>
          )}
        </div>
        <div className="flex items-center gap-2">
          <ThemeToggle />
          {isMobile && onClose ? (
            <Button
              onClick={onClose}
              variant="ghost"
              size="icon"
              aria-label="Close navigation menu"
              className="h-10 w-10 hover:bg-muted/60 transition-all duration-200"
            >
              <X className="h-4 w-4" />
            </Button>
          ) : (
            <Button
              onClick={onLogout}
              variant="ghost"
              size="icon"
              aria-label="Logout"
              className="h-10 w-10 hover:bg-destructive/10 hover:text-destructive transition-all duration-200"
            >
              <LogOut className="h-4 w-4" />
            </Button>
          )}
        </div>
      </div>
    </div>
  );
};
