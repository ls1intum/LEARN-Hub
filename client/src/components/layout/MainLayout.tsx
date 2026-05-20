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
import { Footer } from "@/components/layout/Footer";
import type { AuthRedirectState } from "@/utils/authRedirect";

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

  const activePath = useMemo(() => {
    return location.pathname;
  }, [location.pathname]);

  const currentTab = useMemo(() => getCurrentTab(activePath), [activePath]);

  const visibleTabs = useMemo(
    () =>
      NAVIGATION_TABS.filter((tab) => {
        if (tab.devOnly && environment === "production") return false;
        // Show auth-required tabs to unauthenticated users (redirect to login on click)
        if (!user && tab.requiresAuth) return true;
        return tab.roles.includes(
          (user?.role as "ADMIN" | "TEACHER" | "GUEST") || "GUEST",
        );
      }),
    [user, environment],
  );

  const authGatedPaths = useMemo(
    () =>
      new Set(
        NAVIGATION_TABS.filter((tab) => tab.requiresAuth && !user).map(
          (tab) => tab.path,
        ),
      ),
    [user],
  );

  const handleNavigation = (path: string) => {
    if (!user) {
      const tab = NAVIGATION_TABS.find((t) => t.path === path);
      if (tab?.requiresAuth) {
        navigate("/login", {
          state: {
            from: { pathname: path, search: "", hash: "" },
            message: t("nav.loginRequiredMessage"),
          } satisfies AuthRedirectState,
        });
        closeMobile();
        return;
      }
    }
    navigate(path);
    closeMobile();
  };

  return (
    <div className="flex h-full overflow-hidden">
      {/* ── Desktop Sidebar ── */}
      <aside
        className={cn(
          "app-sidebar hidden shrink-0 border-r border-border/70 lg:flex lg:flex-col",
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
            authGatedPaths={authGatedPaths}
          />
        </div>

        {/* Collapse toggle */}
        <div className="border-t border-border px-2 py-2">
          <button
            onClick={toggleCollapsed}
            className={cn(
              "flex w-full items-center gap-2 rounded-xl p-2 text-sm text-muted-foreground transition-colors hover:bg-background/70 hover:text-foreground",
              isCollapsed && "justify-center",
            )}
            aria-label={
              isCollapsed
                ? t("header.expandSidebar")
                : t("header.collapseSidebar")
            }
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
          "app-sidebar fixed left-0 top-0 z-50 h-full w-64 border-r border-border/70 shadow-2xl",
          "transform transition-transform duration-300 ease-in-out lg:hidden flex flex-col",
          isMobileOpen ? "translate-x-0" : "-translate-x-full",
        )}
      >
        {/* Drawer header — matches AppShell header style */}
        <div className="app-header flex h-[3.75rem] shrink-0 items-center justify-between px-4 text-primary-foreground">
          <Link
            to="/"
            className="flex items-center gap-2 hover:opacity-90 transition-opacity"
            onClick={closeMobile}
          >
            <img
              src="/logo.png"
              alt="LEARN-Hub"
              className="h-7 w-7 rounded-lg border border-white/20 bg-white/10 p-0.5"
            />
            <span className="font-bold text-sm tracking-tight">LEARN-Hub</span>
          </Link>
          <button
            onClick={closeMobile}
            className="control-chrome rounded-full p-1.5"
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
            authGatedPaths={authGatedPaths}
          />
        </div>
      </div>

      {/* ── Main Content ── */}
      <main
        id={APP_SCROLL_CONTAINER_ID}
        className="app-gradient flex min-w-0 flex-1 flex-col overflow-x-hidden overflow-y-auto"
      >
        <div
          className={cn(
            "flex-1 p-4 sm:p-6 lg:p-8",
            !fullWidth && "max-w-6xl mx-auto w-full",
          )}
        >
          {children}
        </div>
        <div className="lg:hidden">
          <Footer />
        </div>
      </main>
    </div>
  );
};
