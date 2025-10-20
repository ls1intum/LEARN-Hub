import type { ComponentType } from "react";
import { LoginPage } from "@/pages/LoginPage";
import { AuthVerifyPage } from "@/pages/AuthVerifyPage";
import { HomePage } from "@/pages/HomePage";
import { RecommendationsPage } from "@/pages/RecommendationsPage";
import { LibraryPage } from "@/pages/LibraryPage";
import { ActivityDetails } from "@/pages/ActivityDetails";
import { UserManagementPage } from "@/pages/UserManagementPage";
import { SearchHistoryPage } from "@/pages/SearchHistoryPage";
import { FavouritesPage } from "@/pages/FavouritesPage";
import { UploadTab } from "@/components/UploadTab";

export type UserRole = "ADMIN" | "TEACHER" | "GUEST";

export interface RouteConfig {
  path: string;
  component: ComponentType;
  requiredRole?: UserRole;
  allowedRoles?: UserRole[];
  isPublic?: boolean;
  title?: string;
}

export const routes: RouteConfig[] = [
  {
    path: "/login",
    component: LoginPage,
    isPublic: true,
    title: "Login",
  },
  {
    path: "/auth/verify",
    component: AuthVerifyPage,
    isPublic: true,
    title: "Verify Account",
  },
  {
    path: "/home",
    component: HomePage,
    requiredRole: "TEACHER",
    title: "Home",
  },
  {
    path: "/recommendations",
    component: RecommendationsPage,
    isPublic: true,
    title: "Recommendations",
  },
  {
    path: "/library",
    component: LibraryPage,
    isPublic: true,
    title: "Library",
  },
  {
    path: "/activity-details/:id",
    component: ActivityDetails,
    isPublic: true,
    title: "Activity Details",
  },
  {
    path: "/favourites",
    component: FavouritesPage,
    allowedRoles: ["TEACHER", "ADMIN"],
    title: "Favourites",
  },
  {
    path: "/history",
    component: SearchHistoryPage,
    allowedRoles: ["TEACHER", "ADMIN"],
    title: "Search History",
  },
  {
    path: "/upload",
    component: UploadTab,
    allowedRoles: ["TEACHER", "ADMIN"],
    title: "Upload Activity",
  },
  {
    path: "/admin/users",
    component: UserManagementPage,
    requiredRole: "ADMIN",
    title: "User Management",
  },
];
