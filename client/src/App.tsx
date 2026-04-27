import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import { AuthProvider } from "@/contexts/AuthContext";
import { ThemeProvider } from "@/contexts/ThemeContext";
import { LanguageProvider } from "@/contexts/LanguageContext";
import { SidebarProvider } from "@/contexts/SidebarContext";
import { EnvironmentProvider } from "@/contexts/EnvironmentContext";
import { TooltipProvider } from "@/components/ui/tooltip";
import { ProtectedRoute } from "@/components/ProtectedRoute";
import { ErrorBoundary } from "@/components/ErrorBoundary";
import { AppShell } from "@/components/layout/AppShell";
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
import { DraftsPage } from "@/pages/DraftsPage";
import { ActivityEditPage } from "@/pages/ActivityEditPage";
import { AccountDashboardPage } from "@/pages/AccountDashboardPage";
import { ImpressumPage } from "@/pages/ImpressumPage";
import { AITestingPage } from "@/pages/AITestingPage";
import { LandingPage } from "@/pages/LandingPage";

// Protected route + sidebar layout combined
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
        <LanguageProvider>
          <AuthProvider>
            <EnvironmentProvider>
              <SidebarProvider>
                <TooltipProvider>
                  <Router>
                    {/* AppShell: shared header + footer, never re-mounts on navigation */}
                    <AppShell>
                      <Routes>
                        <Route path="/" element={<LandingPage />} />
                        <Route path="/login" element={<LoginPage />} />
                        <Route
                          path="/auth/verify"
                          element={<AuthVerifyPage />}
                        />
                        <Route
                          path="/home"
                          element={
                            <ProtectedRoute requiredRole="TEACHER">
                              <HomePage />
                            </ProtectedRoute>
                          }
                        />

                        {/* Public sidebar routes */}
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
                        <Route
                          path="/impressum"
                          element={
                            <MainLayout>
                              <ImpressumPage />
                            </MainLayout>
                          }
                        />

                        {/* Protected sidebar routes */}
                        <Route
                          path="/favourites"
                          element={
                            <ProtectedLayout
                              allowedRoles={["TEACHER", "ADMIN"]}
                            >
                              <FavouritesPage />
                            </ProtectedLayout>
                          }
                        />
                        <Route
                          path="/history"
                          element={
                            <ProtectedLayout
                              allowedRoles={["TEACHER", "ADMIN"]}
                            >
                              <SearchHistoryPage />
                            </ProtectedLayout>
                          }
                        />
                        <Route
                          path="/drafts"
                          element={
                            <ProtectedLayout requiredRole="ADMIN">
                              <DraftsPage />
                            </ProtectedLayout>
                          }
                        />
                        <Route
                          path="/activity-edit/:id"
                          element={
                            <ProtectedLayout requiredRole="ADMIN">
                              <ActivityEditPage />
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
                        <Route
                          path="/account"
                          element={
                            <ProtectedLayout
                              allowedRoles={["ADMIN", "TEACHER", "GUEST"]}
                            >
                              <AccountDashboardPage />
                            </ProtectedLayout>
                          }
                        />
                        <Route
                          path="/ai-testing"
                          element={
                            <ProtectedLayout requiredRole="ADMIN">
                              <AITestingPage />
                            </ProtectedLayout>
                          }
                        />
                      </Routes>
                    </AppShell>
                  </Router>
                </TooltipProvider>
              </SidebarProvider>
            </EnvironmentProvider>
          </AuthProvider>
        </LanguageProvider>
      </ThemeProvider>
    </ErrorBoundary>
  );
}

export default App;
