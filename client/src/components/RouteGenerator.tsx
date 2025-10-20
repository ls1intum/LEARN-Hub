import React from "react";
import { Routes, Route } from "react-router-dom";
import { ProtectedRoute } from "./ProtectedRoute";
import { MainLayout } from "./layout/MainLayout";
import { routes, type RouteConfig } from "@/config/routes";

interface RouteGeneratorProps {
  userRole?: string;
}

const RouteWrapper: React.FC<{ config: RouteConfig }> = ({ config }) => {
  const { component, requiredRole, allowedRoles, isPublic } = config;

  const Component = component;

  if (isPublic) {
    return (
      <MainLayout>
        <Component />
      </MainLayout>
    );
  }

  return (
    <ProtectedRoute requiredRole={requiredRole} allowedRoles={allowedRoles}>
      <MainLayout>
        <Component />
      </MainLayout>
    </ProtectedRoute>
  );
};

export const RouteGenerator: React.FC<RouteGeneratorProps> = () => {
  return (
    <Routes>
      {routes.map((route) => (
        <Route
          key={route.path}
          path={route.path}
          element={<RouteWrapper config={route} />}
        />
      ))}
    </Routes>
  );
};
