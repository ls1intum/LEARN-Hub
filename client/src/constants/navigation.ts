import {
  Home,
  Library,
  Users,
  History,
  Heart,
  FlaskConical,
  FilePenLine,
  type LucideIcon,
} from "lucide-react";

export interface NavigationTab {
  id: string;
  label: string;
  path: string;
  icon: LucideIcon;
  roles: ("ADMIN" | "TEACHER" | "GUEST")[];
  /** If true, hidden in production environments */
  devOnly?: boolean;
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
    id: "drafts",
    label: "Drafts",
    path: "/drafts",
    icon: FilePenLine,
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
    id: "ai-testing",
    label: "AI Testing",
    path: "/ai-testing",
    icon: FlaskConical,
    roles: ["ADMIN"],
    devOnly: true,
  },
];

export const getCurrentTab = (path: string): string => {
  const normalizedPath = path.split("?")[0].split("#")[0];
  const tab = NAVIGATION_TABS.find((tab) => tab.path === normalizedPath);
  return tab?.id || "";
};
