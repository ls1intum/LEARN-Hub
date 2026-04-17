import React from "react";
import { NavigationItem } from "./NavigationItem";

interface Tab {
  id: string;
  label: string;
  path: string;
  icon: React.ComponentType<{ className?: string }>;
  roles: string[];
}

interface NavigationMenuProps {
  tabs: Tab[];
  currentTab: string;
  onNavigation: (path: string) => void;
  collapsed?: boolean;
  className?: string;
}

export const NavigationMenu: React.FC<NavigationMenuProps> = ({
  tabs,
  currentTab,
  onNavigation,
  collapsed = false,
  className = "",
}) => {
  return (
    <nav
      className={`px-2 py-4 ${className}`}
      aria-label="Main navigation"
    >
      <div className="space-y-1" role="list">
        {tabs.map((tab) => (
          <NavigationItem
            key={tab.id}
            tab={tab}
            isActive={currentTab === tab.id}
            onClick={onNavigation}
            collapsed={collapsed}
          />
        ))}
      </div>
    </nav>
  );
};
