import React from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "@/hooks/useAuth";
import { ThemeToggle } from "@/components/ThemeToggle";
import { LanguageSwitcher } from "@/components/LanguageSwitcher";
import { Footer } from "@/components/layout/Footer";
import { Button } from "@/components/ui/button";
import { useSidebar } from "@/contexts/SidebarContext";
import { useEnvironment } from "@/hooks/useEnvironment";
import { getEnvironmentDisplayText } from "@/utils/environment";
import { useTranslation } from "react-i18next";
import { Menu, LogIn, LogOut, Settings } from "lucide-react";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuLabel,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";

/* ─────────────────────────────────────────────
   User helpers
───────────────────────────────────────────── */
function getUserInitials(
  user: {
    firstName?: string;
    lastName?: string;
    email?: string;
  } | null,
): string {
  if (user?.firstName && user?.lastName)
    return `${user.firstName[0]}${user.lastName[0]}`.toUpperCase();
  if (user?.firstName) return user.firstName[0].toUpperCase();
  if (user?.email) return user.email[0].toUpperCase();
  return "G";
}

function getUserDisplayName(
  user: {
    firstName?: string;
    lastName?: string;
    email?: string;
  } | null,
): string {
  if (user?.firstName && user?.lastName)
    return `${user.firstName} ${user.lastName}`;
  if (user?.firstName) return user.firstName;
  if (user?.email) return user.email;
  return "Guest";
}

/* ─────────────────────────────────────────────
   Round avatar placeholder
───────────────────────────────────────────── */
const AvatarCircle: React.FC<{ initials: string }> = ({ initials }) => (
  <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-primary-foreground/20 text-primary-foreground text-xs font-semibold ring-2 ring-primary-foreground/30">
    {initials}
  </div>
);

/* ─────────────────────────────────────────────
   User Avatar Dropdown
───────────────────────────────────────────── */
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

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <button
          className="rounded-full hover:ring-2 hover:ring-primary-foreground/40 transition-all outline-none focus-visible:ring-2 focus-visible:ring-primary-foreground/50"
          aria-label={t("header.userMenu")}
        >
          <AvatarCircle initials={initials} />
        </button>
      </DropdownMenuTrigger>
      <DropdownMenuContent side="bottom" align="end" className="w-56">
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

/* ─────────────────────────────────────────────
   Shared Header  (AET guideline: max h-[3.75rem], primary color)
   Identical on landing page and all app routes.
───────────────────────────────────────────── */
const SharedHeader: React.FC = () => {
  const { t } = useTranslation();
  const { user, isAuthenticated, logout } = useAuth();
  const navigate = useNavigate();
  const { hasSidebar, toggleMobile } = useSidebar();
  const { environment } = useEnvironment();
  const envLabel =
    environment && environment !== "production"
      ? ` · ${getEnvironmentDisplayText(environment)}`
      : "";

  const isGuest = !isAuthenticated;

  const handleLogout = async () => {
    await logout();
    navigate("/login");
  };

  return (
    <header className="h-[3.75rem] bg-primary text-primary-foreground shadow-sm shrink-0 z-50 relative">
      <div className="h-full px-3 sm:px-4 flex items-center gap-2">
        {/* Burger — mobile only, only when a sidebar route is active */}
        {hasSidebar && (
          <button
            onClick={toggleMobile}
            className="lg:hidden p-1.5 rounded-md hover:bg-primary-foreground/10 transition-colors"
            aria-label={t("header.toggleNav")}
          >
            <Menu className="h-5 w-5" />
          </button>
        )}

        {/* Logo — always links back to landing */}
        <Link
          to="/"
          className="flex items-center gap-2 hover:opacity-90 transition-opacity shrink-0"
        >
          <img
            src="/logo.png"
            alt="LEARN-Hub"
            className="h-7 w-7 rounded-md"
          />
          <span className="font-bold text-sm sm:text-base tracking-tight leading-none">
            LEARN-Hub
          </span>
          <span className="text-[10px] font-medium opacity-60 leading-none hidden sm:block">
            {t("header.version")}{envLabel}
          </span>
        </Link>

        {/* Nav links — desktop only */}
        <nav className="hidden md:flex items-center gap-0.5 ml-3" aria-label="Global">
          <Link
            to="/library"
            className="px-3 py-1.5 text-sm font-medium rounded-md hover:bg-primary-foreground/10 transition-colors text-primary-foreground/90 hover:text-primary-foreground"
          >
            {t("nav.library")}
          </Link>
          <Link
            to="/impressum"
            className="px-3 py-1.5 text-sm font-medium rounded-md hover:bg-primary-foreground/10 transition-colors text-primary-foreground/90 hover:text-primary-foreground"
          >
            {t("header.about")}
          </Link>
        </nav>

        {/* Spacer */}
        <div className="flex-1" />

        {/* Controls */}
        <div className="flex items-center gap-1">
          <LanguageSwitcher />
          <ThemeToggle />

          {isGuest ? (
            <Button
              size="sm"
              onClick={() => navigate("/login")}
              className="ml-1 bg-primary-foreground text-primary hover:bg-primary-foreground/90 h-8 px-3 text-sm font-medium"
            >
              <LogIn className="h-3.5 w-3.5 mr-1.5" />
              {t("login.loginButton")}
            </Button>
          ) : (
            <div className="ml-1">
              <UserAvatarDropdown
                user={user}
                onLogout={handleLogout}
                onAccountSettings={() => navigate("/account")}
              />
            </div>
          )}
        </div>
      </div>
    </header>
  );
};

/* ─────────────────────────────────────────────
   App Shell
   Fixed viewport height. Header and footer never re-mount
   when navigating between app routes.

   Footer is shown at shell level only for app routes (hasSidebar).
   The landing page renders its own Footer inside the scroll container.
───────────────────────────────────────────── */
export const AppShell: React.FC<{ children: React.ReactNode }> = ({
  children,
}) => {
  const { hasSidebar } = useSidebar();

  return (
    <div className="h-screen flex flex-col overflow-hidden">
      <SharedHeader />

      {/* Routes render here — each route controls its own scroll */}
      <div className="flex-1 min-h-0">{children}</div>

      {/* Footer is always visible on app routes (sidebar layout).
          The landing page manages its own footer below the fold. */}
      {hasSidebar && <Footer />}
    </div>
  );
};
