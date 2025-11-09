import React, { useMemo, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { useAuth } from "@/hooks/useAuth";
import { useEnvironment } from "@/hooks/useEnvironment";
import { Menu, Server } from "lucide-react";
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
}

export const MainLayout: React.FC<MainLayoutProps> = ({ children }) => {
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
    <div className="min-h-screen bg-gradient-to-br from-background via-background to-muted/20 flex">
      {/* Left Sidebar */}
      <div className="w-72 h-screen border-r border-border/50 bg-card/80 backdrop-blur-xl hidden lg:block shadow-xl flex flex-col">
        <UserHeader user={user ?? { role: "GUEST" }} onLogout={handleLogout} />
        <div className="flex-1 overflow-y-auto">
          <NavigationMenu
            tabs={visibleTabs}
            currentTab={currentTab}
            onNavigation={handleNavigation}
          />
        </div>
        {/* Environment Version Footer - Sticks to bottom */}
        <div className="mt-auto p-4 border-t border-border/50 bg-card/50">
          <div className="flex items-center justify-center gap-2">
            <Server className="h-3.5 w-3.5 text-muted-foreground" />
            {environment && (
              <Badge
                variant={getEnvironmentBadgeVariant(environment)}
                className="text-xs font-medium px-2.5 py-1"
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
          className="fixed inset-0 bg-black/50 backdrop-blur-sm z-40 lg:hidden"
          onClick={() => setIsMobileNavOpen(false)}
        />
      )}

      {/* Mobile Navigation Drawer */}
      <div
        className={cn(
          "fixed top-0 left-0 h-full w-80 bg-card/95 backdrop-blur-xl border-r border-border/50 shadow-2xl z-50 transform transition-transform duration-300 ease-in-out lg:hidden flex flex-col",
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
        <div className="mt-auto p-6 border-t border-border/50 space-y-4 bg-card/50">
          <Button
            onClick={handleLogout}
            variant="ghost"
            className="w-full flex items-center gap-3 px-4 py-4 text-sm font-medium rounded-2xl hover:bg-destructive/10 hover:text-destructive transition-all duration-200"
          >
            <span>Logout</span>
          </Button>
          {/* Environment Version */}
          <div className="flex items-center justify-center gap-2">
            <Server className="h-3.5 w-3.5 text-muted-foreground" />
            {environment && (
              <Badge
                variant={getEnvironmentBadgeVariant(environment)}
                className="text-xs font-medium px-2.5 py-1"
              >
                {getEnvironmentDisplayText(environment)}
              </Badge>
            )}
          </div>
        </div>
      </div>

      {/* Main Content */}
      <div className="flex-1 flex flex-col">
        {/* Mobile Header */}
        <div className="lg:hidden p-6 border-b border-border/50 bg-card/80 backdrop-blur-xl shadow-lg">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <Button
                onClick={toggleMobileNav}
                variant="ghost"
                size="icon"
                aria-label="Toggle navigation menu"
                className="h-10 w-10 hover:bg-muted/60 transition-all duration-200"
              >
                <Menu className="h-5 w-5" />
              </Button>
              <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-primary to-primary/80 flex items-center justify-center shadow-md">
                <span className="text-primary-foreground font-bold text-sm">
                  {isAdmin ? "A" : isGuest ? "G" : "T"}
                </span>
              </div>
              <div>
                <h1 className="text-lg font-bold text-foreground">
                  {isAdmin
                    ? "Admin Panel"
                    : isGuest
                      ? "Guest Access"
                      : "Teaching Hub"}
                </h1>
                {user?.email && (
                  <p className="text-xs text-muted-foreground">{user.email}</p>
                )}
                {isGuest && !user?.email && (
                  <p className="text-xs text-muted-foreground">Guest User</p>
                )}
              </div>
            </div>
            <div className="flex items-center gap-2">
              <Button
                onClick={handleLogout}
                variant="ghost"
                size="icon"
                aria-label="Logout"
                className="h-10 w-10 hover:bg-destructive/10 hover:text-destructive transition-all duration-200"
              >
                <span>Logout</span>
              </Button>
            </div>
          </div>
        </div>

        <main className="flex-1 p-4 lg:p-6 overflow-x-hidden overflow-y-auto bg-gradient-to-br from-background/50 via-background/30 to-muted/10">
          <div className="w-full max-w-6xl mx-auto px-2 sm:px-4">
            {children}
          </div>
        </main>
      </div>
    </div>
  );
};
