import React from "react";
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
  };
  isActive: boolean;
  onClick: (path: string) => void;
  collapsed?: boolean;
  className?: string;
}

export const NavigationItem: React.FC<NavigationItemProps> = ({
  tab,
  isActive,
  onClick,
  collapsed = false,
  className = "",
}) => {
  const { t } = useTranslation();
  const Icon = tab.icon;

  const button = (
    <button
      onClick={() => onClick(tab.path)}
      className={cn(
        "w-full flex items-center gap-3 py-2.5 text-sm font-medium rounded-lg transition-colors duration-150 group relative",
        collapsed ? "justify-center px-2" : "px-3",
        isActive
          ? "bg-primary text-primary-foreground"
          : "text-muted-foreground hover:text-foreground hover:bg-muted",
        className,
      )}
      aria-current={isActive ? "page" : undefined}
      role="listitem"
    >
      <Icon
        className={cn(
          "h-4 w-4 flex-shrink-0",
          isActive
            ? "text-primary-foreground"
            : "text-muted-foreground group-hover:text-foreground",
        )}
        aria-hidden="true"
      />
      {!collapsed && <span>{t(`nav.${tab.id}`)}</span>}
      {!collapsed && isActive && (
        <div className="absolute right-3 w-1.5 h-1.5 bg-primary-foreground/70 rounded-full" />
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
