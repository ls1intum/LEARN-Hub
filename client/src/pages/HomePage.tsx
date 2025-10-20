import React from "react";
import { Navigate } from "react-router-dom";

export const HomePage: React.FC = () => {
  // Redirect to recommendation form
  return <Navigate to="/recommendations" replace />;
};
