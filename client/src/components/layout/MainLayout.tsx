import React, { useMemo, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { useAuth } from "@/hooks/useAuth";
import { useEnvironment } from "@/hooks/useEnvironment";
import { Menu, Server, LogOut, LogIn, Settings, ChevronsUpDown } from "lucide-react";
import { cn } from "@/lib/utils";
import { NAVIGATION_TABS, getCurrentTab } from "@/constants/navigation";
import { NavigationMenu } from "./NavigationMenu";
import { UserHeader } from "./UserHeader";
import { Footer } from "./Footer";
import { ThemeToggle } from "@/components/ThemeToggle";
import { LanguageSwitcher } from "@/components/LanguageSwitcher";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuLabel,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  getEnvironmentDisplayText,
  getEnvironmentBadgeVariant,
} from "@/utils/environment";
import { useTranslation } from "react-i18next";

interface MainLayoutProps {
  children: React.ReactNode;
  /** When true, content area stretches full-width (used for markdown editor) */
  fullWidth?: boolean;
}

function getUserInitials(user: {
  firstName?: string;
  lastName?: string;
  email?: string;
  role?: string;
} | null): string {
  if (user?.firstName && user?.lastName) {
    return `${user.firstName[0]}${user.lastName[0]}`.toUpperCase();
  }
  if (user?.firstName) return user.firstName[0].toUpperCase();
  if (user?.email) return user.email[0].toUpperCase();
  if (user?.role === "GUEST") return "G";
  return "U";
}

function getUserDisplayName(user: {
  firstName?: string;
  lastName?: string;
  email?: string;
  role?: string;
} | null): string {
  if (user?.firstName && user?.lastName) {
    return `${user.firstName} ${user.lastName}`;
  }
  if (user?.firstName) return user.firstName;
  if (user?.email) return user.email;
  return "Guest";
}

function getUserRoleLabel(
  role: string,
  t: (key: string) => string,
): string {
  switch (role) {
    case "ADMIN":
      return t("userHeader.adminPanel");
    case "TEACHER":
      return t("userHeader.teachingHub");
    default:
      return t("userHeader.guestAccess");
  }
}

interface UserAvatarDropdownProps {
  user: {
    firstName?: string;
    lastName?: string;
    email?: string;
    role?: string;
  } | null;
  onLogout: () => void;
  onAccountSettings: () => void;
}

const UserAvatarDropdown: React.FC<UserAvatarDropdownProps> = ({
  user,
  onLogout,
  onAccountSettings,
}) => {
  const { t } = useTranslation();
  const initials = getUserInitials(user);
  const displayName = getUserDisplayName(user);
  const role = getUserRoleLabel(user?.role || "GUEST", t);

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <button className="flex w-full items-center gap-2 rounded-md p-2 text-left text-sm hover:bg-accent transition-colors outline-none focus-visible:ring-2 focus-visible:ring-ring">
          <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-primary text-primary-foreground text-xs font-bold">
            {initials}
          </div>
          <div className="flex-1 min-w-0">
            <p className="truncate text-sm font-medium leading-tight">
              {displayName}
            </p>
            <p className="truncate text-xs text-muted-foreground leading-tight">
              {role}
            </p>
          </div>
          <ChevronsUpDown className="h-4 w-4 shrink-0 text-muted-foreground" />
        </button>
      </DropdownMenuTrigger>
      <DropdownMenuContent side="top" align="start" className="w-56">
        <DropdownMenuLabel className="font-normal">
          <div className="flex flex-col space-y-1">
            <p className="text-sm font-medium leading-none">{displayName}</p>
            {user?.email && (
              <p className="text-xs leading-none text-muted-foreground">
                {user.email}
              </p>
            )}
          </div>
        </DropdownMenuLabel>
        <DropdownMenuSeparator />
        <DropdownMenuItem onClick={onAccountSettings}>
          <Settings className="h-4 w-4" />
          {t("sidebar.accountSettings")}
        </DropdownMenuItem>
        <DropdownMenuSeparator />
        <DropdownMenuItem onClick={onLogout}>
          <LogOut className="h-4 w-4" />
          {t("sidebar.logout")}
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
};

export const MainLayout: React.FC<MainLayoutProps> = ({
  children,
  fullWidth = false,
}) => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [isMobileNavOpen, setIsMobileNavOpen] = useState(false);
  const { environment } = useEnvironment();
  const { t } = useTranslation();

  const handleLogout = async () => {
    await logout();
    navigate("/login");
  };

  const handleAccountSettings = () => {
    navigate("/account");
    setIsMobileNavOpen(false);
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
        <UserHeader user={user ?? { role: "GUEST" }} />

        {/* Navigation */}
        <div className="flex-1 overflow-y-auto">
          <NavigationMenu
            tabs={visibleTabs}
            currentTab={currentTab}
            onNavigation={handleNavigation}
          />
        </div>

        {/* Theme & Language Switches */}
        <div className="px-3 pb-2 flex items-center justify-center gap-1">
          <LanguageSwitcher />
          <ThemeToggle />
        </div>

        {/* User Avatar with Dropdown / Guest Login Link */}
        <div className="border-t border-border p-2">
          {isGuest ? (
            <button
              onClick={() => navigate("/login")}
              className="flex w-full items-center gap-2 rounded-md p-2 text-left text-sm hover:bg-accent transition-colors"
            >
              <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-muted text-muted-foreground">
                <LogIn className="h-4 w-4" />
              </div>
              <div className="flex-1 min-w-0">
                <p className="truncate text-sm font-medium leading-tight">
                  {t("login.loginButton")}
                </p>
                <p className="truncate text-xs text-muted-foreground leading-tight">
                  {t("sidebar.loginPrompt")}
                </p>
              </div>
            </button>
          ) : (
            <UserAvatarDropdown
              user={user ?? { role: "GUEST" }}
              onLogout={handleLogout}
              onAccountSettings={handleAccountSettings}
            />
          )}
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

        {/* Theme & Language Switches */}
        <div className="px-4 pb-2 flex items-center justify-center gap-1">
          <LanguageSwitcher />
          <ThemeToggle />
        </div>

        {/* Mobile User Avatar with Dropdown / Guest Login Link */}
        <div className="border-t border-border p-2">
          {isGuest ? (
            <button
              onClick={() => { navigate("/login"); setIsMobileNavOpen(false); }}
              className="flex w-full items-center gap-2 rounded-md p-2 text-left text-sm hover:bg-accent transition-colors"
            >
              <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-muted text-muted-foreground">
                <LogIn className="h-4 w-4" />
              </div>
              <div className="flex-1 min-w-0">
                <p className="truncate text-sm font-medium leading-tight">
                  {t("login.loginButton")}
                </p>
                <p className="truncate text-xs text-muted-foreground leading-tight">
                  {t("sidebar.loginPrompt")}
                </p>
              </div>
            </button>
          ) : (
            <UserAvatarDropdown
              user={user ?? { role: "GUEST" }}
              onLogout={handleLogout}
              onAccountSettings={handleAccountSettings}
            />
          )}
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
              <div>
                <h1 className="text-sm font-bold text-foreground leading-tight">
                  LEARN-Hub
                </h1>
                <p className="text-xs text-muted-foreground leading-tight">
                  {isAdmin
                    ? t("sidebar.adminView")
                    : isGuest
                      ? t("sidebar.guestView")
                      : t("sidebar.teacherView")}
                </p>
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

        <main className="flex-1 flex flex-col overflow-x-hidden overflow-y-auto">
          <div
            className={cn(
              "flex-1 p-4 sm:p-6 lg:p-8",
              !fullWidth && "max-w-6xl mx-auto w-full",
            )}
          >
            {children}
          </div>
          <Footer />
          {/* Environment tiny footer */}
          <div className="py-2 text-center">
            <div className="inline-flex items-center gap-1.5">
              <Server className="h-3 w-3 text-muted-foreground" />
              {environment && (
                <Badge
                  variant={getEnvironmentBadgeVariant(environment)}
                  className="text-[10px] font-medium px-1.5 py-0"
                >
                  {getEnvironmentDisplayText(environment)}
                </Badge>
              )}
            </div>
          </div>
        </main>
      </div>
    </div>
  );
};
