import React, { createContext, useEffect, useState } from "react";
import type { ReactNode } from "react";
import { authService } from "@/services/authService";
import type { User } from "@/services/authService";
import { logger } from "@/services/logger";

export interface AuthContextType {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (
    email: string,
    password: string,
  ) => Promise<{ success: boolean; message?: string; user?: User }>;
  teacherLogin: (
    email: string,
    password: string,
  ) => Promise<{ success: boolean; message?: string; user?: User }>;
  verificationCodeLogin: (
    code: string,
    email: string,
  ) => Promise<{ success: boolean; message?: string; user?: User }>;
  requestVerificationCode: (
    email: string,
  ) => Promise<{ success: boolean; message?: string }>;
  registerTeacher: (
    email: string,
    firstName: string,
    lastName: string,
  ) => Promise<{ success: boolean; message?: string; user?: User }>;
  resetPassword: (
    email: string,
  ) => Promise<{ success: boolean; message?: string }>;
  guestLogin: () => Promise<{
    success: boolean;
    message?: string;
    user?: User;
  }>;
  logout: () => Promise<void>;
  refreshUser: () => Promise<void>;
  updateProfile: (
    data: import("@/types/api").UpdateProfileRequest,
  ) => Promise<{ success: boolean; message?: string }>;
  deleteAccount: () => Promise<{ success: boolean; message?: string }>;
}

export const AuthContext = createContext<AuthContextType | undefined>(
  undefined,
);

interface AuthProviderProps {
  children: ReactNode;
}

export const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  const isAuthenticated = !!user;

  useEffect(() => {
    const initializeAuth = async () => {
      if (authService.isAuthenticated()) {
        try {
          const currentUser = await authService.getCurrentUser();
          if (currentUser) {
            setUser(currentUser);
          } else {
            // If we can't get user info, clear tokens
            authService.clearTokens();
            setUser(null);
          }
        } catch (error) {
          logger.error("Error getting current user", error, "AuthContext");
          // Clear invalid tokens
          authService.clearTokens();
          setUser(null);
        }
      } else {
        setUser(null);
      }
      setIsLoading(false);
    };

    initializeAuth();
  }, []);

  const login = async (email: string, password: string) => {
    const result = await authService.adminLogin(email, password);
    if (result.success && result.user) {
      setUser(result.user);
    }
    return result;
  };

  const teacherLogin = async (email: string, password: string) => {
    const result = await authService.teacherLogin(email, password);
    if (result.success && result.user) {
      setUser(result.user);
    }
    return result;
  };

  const verificationCodeLogin = async (code: string, email: string) => {
    const result = await authService.verificationCodeLogin(code, email);
    if (result.success && result.user) {
      setUser(result.user);
    }
    return result;
  };

  const requestVerificationCode = async (email: string) => {
    return await authService.requestVerificationCode(email);
  };

  const registerTeacher = async (
    email: string,
    firstName: string,
    lastName: string,
  ) => {
    return await authService.registerTeacher(email, firstName, lastName);
  };

  const resetPassword = async (email: string) => {
    return await authService.resetPassword(email);
  };

  const guestLogin = async () => {
    const result = await authService.guestLogin();
    if (result.success && result.user) {
      setUser(result.user);
    }
    return result;
  };

  const logout = async () => {
    await authService.logout();
    setUser(null);
  };

  const refreshUser = async () => {
    if (!authService.isAuthenticated()) {
      setUser(null);
      return;
    }

    try {
      const currentUser = await authService.getCurrentUser();
      if (currentUser) {
        setUser(currentUser);
      } else {
        // If we can't get user info, clear tokens
        authService.clearTokens();
        setUser(null);
      }
    } catch (error) {
      logger.error("Error refreshing user", error, "AuthContext");
      // Clear invalid tokens
      authService.clearTokens();
      setUser(null);
    }
  };

  const updateProfile = async (
    data: import("@/types/api").UpdateProfileRequest,
  ) => {
    try {
      const { apiService } = await import("@/services/apiService");
      await apiService.updateProfile(data);

      // Refresh user data to get updated information
      await refreshUser();

      return { success: true, message: "Profile updated successfully" };
    } catch (error) {
      logger.error("Error updating profile", error, "AuthContext");
      return {
        success: false,
        message:
          error instanceof Error ? error.message : "Failed to update profile",
      };
    }
  };

  const deleteAccount = async () => {
    try {
      const { apiService } = await import("@/services/apiService");
      await apiService.deleteProfile();

      // Clear tokens and user state
      authService.clearTokens();
      setUser(null);

      return { success: true, message: "Account deleted successfully" };
    } catch (error) {
      logger.error("Error deleting account", error, "AuthContext");
      return {
        success: false,
        message:
          error instanceof Error ? error.message : "Failed to delete account",
      };
    }
  };

  const value: AuthContextType = {
    user,
    isAuthenticated,
    isLoading,
    login,
    teacherLogin,
    verificationCodeLogin,
    requestVerificationCode,
    registerTeacher,
    resetPassword,
    guestLogin,
    logout,
    refreshUser,
    updateProfile,
    deleteAccount,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};
