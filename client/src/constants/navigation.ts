import {
  Home,
  Library,
  Plus,
  Users,
  History,
  Heart,
  User,
  type LucideIcon,
} from "lucide-react";

export interface NavigationTab {
  id: string;
  label: string;
  path: string;
  icon: LucideIcon;
  roles: ("ADMIN" | "TEACHER" | "GUEST")[];
}

export const NAVIGATION_TABS: NavigationTab[] = [
  {
    id: "recommendations",
    label: "Recommendations",
    path: "/recommendations",
    icon: Home,
    roles: ["ADMIN", "TEACHER", "GUEST"],
  },
  {
    id: "library",
    label: "Library",
    path: "/library",
    icon: Library,
    roles: ["ADMIN", "TEACHER", "GUEST"],
  },
  {
    id: "favourites",
    label: "Favourites",
    path: "/favourites",
    icon: Heart,
    roles: ["ADMIN", "TEACHER"],
  },
  {
    id: "history",
    label: "History",
    path: "/history",
    icon: History,
    roles: ["ADMIN", "TEACHER"],
  },
  {
    id: "upload",
    label: "Upload",
    path: "/upload",
    icon: Plus,
    roles: ["ADMIN"],
  },
  {
    id: "users",
    label: "Users",
    path: "/users",
    icon: Users,
    roles: ["ADMIN"],
  },
  {
    id: "account",
    label: "Account",
    path: "/account",
    icon: User,
    roles: ["ADMIN", "TEACHER", "GUEST"],
  },
];

export const getCurrentTab = (path: string): string => {
  const tab = NAVIGATION_TABS.find((tab) => tab.path === path);
  return tab?.id || "recommendations";
};
