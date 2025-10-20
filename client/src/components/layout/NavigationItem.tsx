import React from "react";
import { cn } from "@/lib/utils";

interface NavigationItemProps {
  tab: {
    id: string;
    label: string;
    path: string;
    icon: React.ComponentType<{ className?: string }>;
  };
  isActive: boolean;
  onClick: (path: string) => void;
  className?: string;
}

export const NavigationItem: React.FC<NavigationItemProps> = ({
  tab,
  isActive,
  onClick,
  className = "",
}) => {
  const Icon = tab.icon;

  return (
    <button
      onClick={() => onClick(tab.path)}
      className={cn(
        "w-full flex items-center gap-4 px-4 py-4 text-sm font-medium rounded-2xl transition-all duration-300 group relative",
        isActive
          ? "bg-gradient-to-r from-primary to-primary/90 text-primary-foreground shadow-lg shadow-primary/25"
          : "text-muted-foreground hover:text-foreground hover:bg-muted/60 hover:shadow-md",
        className,
      )}
      aria-current={isActive ? "page" : undefined}
      role="listitem"
    >
      <div
        className={cn(
          "flex items-center justify-center w-8 h-8 rounded-lg transition-all duration-300",
          isActive
            ? "bg-primary-foreground/20"
            : "bg-muted/50 group-hover:bg-muted/80",
        )}
      >
        <Icon
          className={cn(
            "h-4 w-4 transition-all duration-300",
            isActive ? "scale-110" : "group-hover:scale-105",
          )}
          aria-hidden="true"
        />
      </div>
      <span className="font-medium">{tab.label}</span>
      {isActive && (
        <div className="absolute right-4 w-2 h-2 bg-primary-foreground rounded-full animate-pulse" />
      )}
    </button>
  );
};
