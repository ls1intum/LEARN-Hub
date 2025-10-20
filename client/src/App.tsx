import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import { AuthProvider } from "@/contexts/AuthContext";
import { ThemeProvider } from "@/contexts/ThemeContext";
import { TooltipProvider } from "@/components/ui/tooltip";
import { ProtectedRoute } from "@/components/ProtectedRoute";
import { RootRoute } from "@/components/RootRoute";
import { ErrorBoundary } from "@/components/ErrorBoundary";
import { MainLayout } from "@/components/layout/MainLayout";
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

// Simple wrapper for protected routes with layout
const ProtectedLayout: React.FC<{
  children: React.ReactNode;
  requiredRole?: "ADMIN" | "TEACHER" | "GUEST";
  allowedRoles?: ("ADMIN" | "TEACHER" | "GUEST")[];
}> = ({ children, requiredRole, allowedRoles }) => (
  <ProtectedRoute requiredRole={requiredRole} allowedRoles={allowedRoles}>
    <MainLayout>{children}</MainLayout>
  </ProtectedRoute>
);

function App() {
  return (
    <ErrorBoundary>
      <ThemeProvider>
        <AuthProvider>
          <TooltipProvider>
            <Router>
              <Routes>
                <Route path="/login" element={<LoginPage />} />
                <Route path="/auth/verify" element={<AuthVerifyPage />} />
                <Route
                  path="/home"
                  element={
                    <ProtectedRoute requiredRole="TEACHER">
                      <HomePage />
                    </ProtectedRoute>
                  }
                />

                {/* Public Access Routes */}
                <Route
                  path="/recommendations"
                  element={
                    <MainLayout>
                      <RecommendationsPage />
                    </MainLayout>
                  }
                />
                <Route
                  path="/library"
                  element={
                    <MainLayout>
                      <LibraryPage />
                    </MainLayout>
                  }
                />
                <Route
                  path="/activity-details/:id"
                  element={
                    <MainLayout>
                      <ActivityDetails />
                    </MainLayout>
                  }
                />

                {/* Protected Routes */}
                <Route
                  path="/favourites"
                  element={
                    <ProtectedLayout allowedRoles={["TEACHER", "ADMIN"]}>
                      <FavouritesPage />
                    </ProtectedLayout>
                  }
                />
                <Route
                  path="/history"
                  element={
                    <ProtectedLayout allowedRoles={["TEACHER", "ADMIN"]}>
                      <SearchHistoryPage />
                    </ProtectedLayout>
                  }
                />
                <Route
                  path="/upload"
                  element={
                    <ProtectedLayout requiredRole="ADMIN">
                      <UploadTab />
                    </ProtectedLayout>
                  }
                />
                <Route
                  path="/users"
                  element={
                    <ProtectedLayout requiredRole="ADMIN">
                      <UserManagementPage />
                    </ProtectedLayout>
                  }
                />

                <Route path="/" element={<RootRoute />} />
              </Routes>
            </Router>
          </TooltipProvider>
        </AuthProvider>
      </ThemeProvider>
    </ErrorBoundary>
  );
}

export default App;
