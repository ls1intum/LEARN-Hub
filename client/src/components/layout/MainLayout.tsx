import React, { useMemo, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { useAuth } from "@/hooks/useAuth";
import { useEnvironment } from "@/hooks/useEnvironment";
import { Menu, Server, LogOut } from "lucide-react";
import { cn } from "@/lib/utils";
import { NAVIGATION_TABS, getCurrentTab } from "@/constants/navigation";
import { NavigationMenu } from "./NavigationMenu";
import { UserHeader } from "./UserHeader";
import {
  getEnvironmentDisplayText,
  getEnvironmentBadgeVariant,
} from "@/utils/environment";

interface MainLayoutProps {
  children: React.ReactNode;
  /** When true, content area stretches full-width (used for markdown editor) */
  fullWidth?: boolean;
}

export const MainLayout: React.FC<MainLayoutProps> = ({
  children,
  fullWidth = false,
}) => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [isMobileNavOpen, setIsMobileNavOpen] = useState(false);
  const { environment } = useEnvironment();

  const handleLogout = async () => {
    await logout();
    navigate("/login");
  };

  const effectiveRole =
    (user?.role as "ADMIN" | "TEACHER" | "GUEST") || "GUEST";
  const isAdmin = effectiveRole === "ADMIN";
  const isGuest = effectiveRole === "GUEST";

  const currentTab = useMemo(
    () => getCurrentTab(location.pathname),
    [location.pathname],
  );

  const visibleTabs = useMemo(
    () =>
      NAVIGATION_TABS.filter((tab) =>
        tab.roles.includes(
          (user?.role as "ADMIN" | "TEACHER" | "GUEST") || "GUEST",
        ),
      ),
    [user?.role],
  );

  const handleNavigation = (path: string) => {
    navigate(path);
    setIsMobileNavOpen(false);
  };

  const toggleMobileNav = () => {
    setIsMobileNavOpen(!isMobileNavOpen);
  };

  return (
    <div className="min-h-screen bg-background flex">
      {/* Left Sidebar */}
      <div className="fixed left-0 top-0 w-64 h-screen border-r border-border bg-card hidden lg:flex flex-col z-30">
        <UserHeader user={user ?? { role: "GUEST" }} onLogout={handleLogout} />

        {/* Navigation */}
        <div className="flex-1 overflow-y-auto">
          <NavigationMenu
            tabs={visibleTabs}
            currentTab={currentTab}
            onNavigation={handleNavigation}
          />
        </div>

        {/* Environment Version Footer */}
        <div className="p-3 border-t border-border">
          <div className="flex items-center justify-center gap-2">
            <Server className="h-3 w-3 text-muted-foreground" />
            {environment && (
              <Badge
                variant={getEnvironmentBadgeVariant(environment)}
                className="text-[10px] font-medium px-2 py-0.5"
              >
                {getEnvironmentDisplayText(environment)}
              </Badge>
            )}
          </div>
        </div>
      </div>

      {/* Mobile Navigation Overlay */}
      {isMobileNavOpen && (
        <div
          className="fixed inset-0 bg-black/40 backdrop-blur-sm z-40 lg:hidden transition-opacity"
          onClick={() => setIsMobileNavOpen(false)}
        />
      )}

      {/* Mobile Navigation Drawer */}
      <div
        className={cn(
          "fixed top-0 left-0 h-full w-72 bg-card border-r border-border shadow-2xl z-50 transform transition-transform duration-300 ease-in-out lg:hidden flex flex-col",
          isMobileNavOpen ? "translate-x-0" : "-translate-x-full",
        )}
      >
        <UserHeader
          user={user ?? { role: "GUEST" }}
          onLogout={handleLogout}
          onClose={() => setIsMobileNavOpen(false)}
          isMobile={true}
        />
        <div className="flex-1 overflow-y-auto">
          <NavigationMenu
            tabs={visibleTabs}
            currentTab={currentTab}
            onNavigation={handleNavigation}
          />
        </div>

        {/* Mobile Nav Footer */}
        <div className="p-4 border-t border-border space-y-3">
          <Button
            onClick={handleLogout}
            variant="ghost"
            className="w-full flex items-center justify-start gap-3 px-3 py-2.5 text-sm font-medium rounded-lg hover:bg-destructive/10 hover:text-destructive transition-colors"
          >
            <LogOut className="h-4 w-4" />
            <span>Logout</span>
          </Button>
          <div className="flex items-center justify-center gap-2">
            <Server className="h-3 w-3 text-muted-foreground" />
            {environment && (
              <Badge
                variant={getEnvironmentBadgeVariant(environment)}
                className="text-[10px] font-medium px-2 py-0.5"
              >
                {getEnvironmentDisplayText(environment)}
              </Badge>
            )}
          </div>
        </div>
      </div>

      {/* Main Content */}
      <div className="flex-1 flex flex-col lg:ml-64">
        {/* Mobile Header */}
        <div className="lg:hidden sticky top-0 z-20 px-4 py-3 border-b border-border bg-card/95 backdrop-blur-md">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <Button
                onClick={toggleMobileNav}
                variant="ghost"
                size="icon"
                aria-label="Toggle navigation menu"
                className="h-9 w-9 hover:bg-muted transition-colors"
              >
                <Menu className="h-5 w-5" />
              </Button>
              <div className="w-7 h-7 rounded-md bg-primary flex items-center justify-center">
                <span className="text-primary-foreground font-bold text-xs">
                  {isAdmin ? "A" : isGuest ? "G" : "T"}
                </span>
              </div>
              <div>
                <h1 className="text-sm font-semibold text-foreground leading-tight">
                  {isAdmin
                    ? "Admin Panel"
                    : isGuest
                      ? "Guest Access"
                      : "Teaching Hub"}
                </h1>
                {user?.email && (
                  <p className="text-[11px] text-muted-foreground leading-tight">
                    {user.email}
                  </p>
                )}
                {isGuest && !user?.email && (
                  <p className="text-[11px] text-muted-foreground leading-tight">
                    Guest User
                  </p>
                )}
              </div>
            </div>
            <Button
              onClick={handleLogout}
              variant="ghost"
              size="icon"
              aria-label="Logout"
              className="h-9 w-9 hover:bg-destructive/10 hover:text-destructive transition-colors"
            >
              <LogOut className="h-4 w-4" />
            </Button>
          </div>
        </div>

        <main className="flex-1 overflow-x-hidden overflow-y-auto">
          <div
            className={cn(
              "p-4 sm:p-6 lg:p-8",
              !fullWidth && "max-w-6xl mx-auto",
            )}
          >
            {children}
          </div>
        </main>
      </div>
    </div>
  );
};
