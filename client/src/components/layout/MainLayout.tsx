import React, { useEffect, useMemo } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "@/hooks/useAuth";
import { useEnvironment } from "@/hooks/useEnvironment";
import { ChevronLeft, ChevronRight, X } from "lucide-react";
import { cn } from "@/lib/utils";
import { NAVIGATION_TABS, getCurrentTab } from "@/constants/navigation";
import { NavigationMenu } from "./NavigationMenu";
import { useSidebar } from "@/contexts/SidebarContext";
import { useTranslation } from "react-i18next";
import { APP_SCROLL_CONTAINER_ID } from "@/utils/scroll";

interface MainLayoutProps {
  children: React.ReactNode;
  fullWidth?: boolean;
}

interface DetailNavigationState {
  backTo?: string;
}

/* ─────────────────────────────────────────────
   Main Layout
   Provides the sidebar for all app routes.
   Header and footer are handled by AppShell.
───────────────────────────────────────────── */
export const MainLayout: React.FC<MainLayoutProps> = ({
  children,
  fullWidth = false,
}) => {
  const { t } = useTranslation();
  const { user } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const { environment } = useEnvironment();
  const {
    isMobileOpen,
    isCollapsed,
    setHasSidebar,
    closeMobile,
    toggleCollapsed,
  } = useSidebar();

  // Register this layout as having a sidebar so AppShell header shows the burger
  useEffect(() => {
    setHasSidebar(true);
    return () => {
      setHasSidebar(false);
      closeMobile();
    };
  }, [setHasSidebar, closeMobile]);

  const detailNavigationState = location.state as DetailNavigationState | null;

  const activePath = useMemo(() => {
    if (
      location.pathname.startsWith("/activity-details/") &&
      detailNavigationState?.backTo
    ) {
      return detailNavigationState.backTo;
    }
    return location.pathname;
  }, [detailNavigationState?.backTo, location.pathname]);

  const currentTab = useMemo(() => getCurrentTab(activePath), [activePath]);

  const visibleTabs = useMemo(
    () =>
      NAVIGATION_TABS.filter((tab) => {
        if (
          !tab.roles.includes(
            (user?.role as "ADMIN" | "TEACHER" | "GUEST") || "GUEST",
          )
        )
          return false;
        if (tab.devOnly && environment === "production") return false;
        return true;
      }),
    [user?.role, environment],
  );

  const handleNavigation = (path: string) => {
    navigate(path);
    closeMobile();
  };

  return (
    <div className="flex h-full overflow-hidden">
      {/* ── Desktop Sidebar ── */}
      <aside
        className={cn(
          "hidden lg:flex flex-col border-r border-border bg-card shrink-0",
          "transition-[width] duration-200 ease-in-out overflow-hidden",
          isCollapsed ? "w-[3.5rem]" : "w-56",
        )}
      >
        <div className="flex-1 overflow-y-auto">
          <NavigationMenu
            tabs={visibleTabs}
            currentTab={currentTab}
            onNavigation={handleNavigation}
            collapsed={isCollapsed}
          />
        </div>

        {/* Collapse toggle */}
        <div className="border-t border-border px-2 py-2">
          <button
            onClick={toggleCollapsed}
            className={cn(
              "flex w-full items-center gap-2 rounded-md p-2 text-sm text-muted-foreground hover:text-foreground hover:bg-muted transition-colors",
              isCollapsed && "justify-center",
            )}
            aria-label={isCollapsed ? t("header.expandSidebar") : t("header.collapseSidebar")}
          >
            {isCollapsed ? (
              <ChevronRight className="h-4 w-4 shrink-0" />
            ) : (
              <>
                <ChevronLeft className="h-4 w-4 shrink-0" />
                <span className="text-xs">{t("header.collapse")}</span>
              </>
            )}
          </button>
        </div>
      </aside>

      {/* ── Mobile Overlay ── */}
      {isMobileOpen && (
        <div
          className="fixed inset-0 bg-black/40 backdrop-blur-sm z-40 lg:hidden"
          onClick={closeMobile}
        />
      )}

      {/* ── Mobile Drawer ── */}
      <div
        className={cn(
          "fixed top-0 left-0 h-full w-64 bg-card border-r border-border shadow-2xl z-50",
          "transform transition-transform duration-300 ease-in-out lg:hidden flex flex-col",
          isMobileOpen ? "translate-x-0" : "-translate-x-full",
        )}
      >
        {/* Drawer header — matches AppShell header style */}
        <div className="h-[3.75rem] bg-primary text-primary-foreground flex items-center justify-between px-4 shrink-0">
          <Link
            to="/"
            className="flex items-center gap-2 hover:opacity-90 transition-opacity"
            onClick={closeMobile}
          >
            <img
              src="/logo.png"
              alt="LEARN-Hub"
              className="h-7 w-7 rounded-md bg-primary-foreground/10 p-0.5"
            />
            <span className="font-bold text-sm tracking-tight">LEARN-Hub</span>
          </Link>
          <button
            onClick={closeMobile}
            className="p-1.5 rounded-md hover:bg-primary-foreground/10 transition-colors"
            aria-label={t("header.closeNav")}
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <div className="flex-1 overflow-y-auto">
          <NavigationMenu
            tabs={visibleTabs}
            currentTab={currentTab}
            onNavigation={handleNavigation}
            collapsed={false}
          />
        </div>
      </div>

      {/* ── Main Content ── */}
      <main
        id={APP_SCROLL_CONTAINER_ID}
        className="flex-1 flex flex-col overflow-x-hidden overflow-y-auto min-w-0"
      >
        <div
          className={cn(
            "flex-1 p-4 sm:p-6 lg:p-8",
            !fullWidth && "max-w-6xl mx-auto w-full",
          )}
        >
          {children}
        </div>
      </main>
    </div>
  );
};
