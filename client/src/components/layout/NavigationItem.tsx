import React from "react";
import { Lock } from "lucide-react";
import { cn } from "@/lib/utils";
import { useTranslation } from "react-i18next";
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from "@/components/ui/tooltip";

interface NavigationItemProps {
  tab: {
    id: string;
    label: string;
    path: string;
    icon: React.ComponentType<{ className?: string }>;
    requiresAuth?: boolean;
  };
  isActive: boolean;
  onClick: (path: string) => void;
  collapsed?: boolean;
  /** If true, shows a lock icon indicating login is required */
  isAuthGated?: boolean;
  className?: string;
}

export const NavigationItem: React.FC<NavigationItemProps> = ({
  tab,
  isActive,
  onClick,
  collapsed = false,
  isAuthGated = false,
  className = "",
}) => {
  const { t } = useTranslation();
  const Icon = tab.icon;

  const button = (
    <button
      onClick={() => onClick(tab.path)}
      className={cn(
        "group relative flex w-full items-center gap-3 rounded-xl py-2.5 text-sm font-medium transition-colors duration-150",
        collapsed ? "justify-center px-2" : "px-3",
        isActive
          ? "nav-active text-foreground"
          : "nav-idle text-muted-foreground hover:text-foreground",
        className,
      )}
      aria-current={isActive ? "page" : undefined}
      role="listitem"
    >
      <Icon
        className={cn(
          "h-4 w-4 flex-shrink-0",
          isActive
            ? "text-primary"
            : "text-muted-foreground group-hover:text-foreground",
        )}
        aria-hidden="true"
      />
      {!collapsed && (
        <span className="flex-1 text-left">{t(`nav.${tab.id}`)}</span>
      )}
      {!collapsed && isAuthGated && (
        <Lock className="h-3 w-3 shrink-0 opacity-50" aria-hidden="true" />
      )}
    </button>
  );

  if (collapsed) {
    return (
      <Tooltip>
        <TooltipTrigger asChild>{button}</TooltipTrigger>
        <TooltipContent side="right" sideOffset={8}>
          {t(`nav.${tab.id}`)}
        </TooltipContent>
      </Tooltip>
    );
  }

  return button;
};
