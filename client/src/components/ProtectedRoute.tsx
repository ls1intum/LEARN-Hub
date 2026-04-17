import React from "react";
import { Navigate, useLocation } from "react-router-dom";
import { useAuth } from "@/hooks/useAuth";
import { getLoginRedirectState } from "@/utils/authRedirect";

interface ProtectedRouteProps {
  children: React.ReactNode;
  requiredRole?: "ADMIN" | "TEACHER" | "GUEST";
  allowedRoles?: ("ADMIN" | "TEACHER" | "GUEST")[];
}

export const ProtectedRoute: React.FC<ProtectedRouteProps> = ({
  children,
  requiredRole,
  allowedRoles,
}) => {
  const { user, isAuthenticated, isLoading } = useAuth();
  const location = useLocation();

  const loginRedirectState = getLoginRedirectState(location);

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary mx-auto mb-4"></div>
          <p className="text-muted-foreground">Loading...</p>
        </div>
      </div>
    );
  }

  if (!isAuthenticated || !user) {
    return <Navigate to="/login" replace state={loginRedirectState} />;
  }

  // Check role-based access
  if (requiredRole && user.role !== requiredRole) {
    return <Navigate to="/login" replace state={loginRedirectState} />;
  }

  // Check allowed roles (for routes that should be accessible to multiple roles)
  if (allowedRoles && !allowedRoles.includes(user.role)) {
    return <Navigate to="/login" replace state={loginRedirectState} />;
  }

  return <>{children}</>;
};
