import {
  describe,
  it,
  expect,
  vi,
  beforeEach,
  beforeAll,
  afterEach,
  afterAll,
} from "vitest";
import { renderHook, waitFor, act } from "@testing-library/react";
import React from "react";
import { AuthProvider, AuthContext } from "../AuthContext";
import { authService } from "@/services/authService";
import { server } from "@/test/mocks/server";
import type { ReactNode } from "react";

// Setup MSW server
beforeAll(() => server.listen({ onUnhandledRequest: "error" }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

// Mock authService
vi.mock("@/services/authService", () => ({
  authService: {
    isAuthenticated: vi.fn(),
    getCurrentUser: vi.fn(),
    clearTokens: vi.fn(),
    adminLogin: vi.fn(),
    teacherLogin: vi.fn(),
    verificationCodeLogin: vi.fn(),
    requestVerificationCode: vi.fn(),
    registerTeacher: vi.fn(),
    resetPassword: vi.fn(),
    guestLogin: vi.fn(),
    logout: vi.fn(),
  },
}));

// Mock logger
vi.mock("@/services/logger", () => ({
  logger: {
    error: vi.fn(),
    info: vi.fn(),
    warn: vi.fn(),
  },
}));

// Mock apiService for profile operations
vi.mock("@/services/apiService", () => ({
  apiService: {
    updateProfile: vi.fn(),
    deleteProfile: vi.fn(),
  },
}));

describe("AuthContext", () => {
  const wrapper = ({ children }: { children: ReactNode }) => (
    <AuthProvider>{children}</AuthProvider>
  );

  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe("Initialization", () => {
    it("should load user from storage when authenticated", async () => {
      const mockUser = {
        id: 1,
        email: "test@example.com",
        role: "TEACHER" as const,
      };
      vi.mocked(authService.isAuthenticated).mockReturnValue(true);
      vi.mocked(authService.getCurrentUser).mockResolvedValue(mockUser);

      const { result } = renderHook(() => React.useContext(AuthContext), {
        wrapper,
      });

      await waitFor(() => {
        expect(result.current?.isLoading).toBe(false);
      });

      expect(result.current?.user).toEqual(mockUser);
      expect(result.current?.isAuthenticated).toBe(true);
      expect(authService.getCurrentUser).toHaveBeenCalledTimes(1);
    });

    it("should clear invalid tokens when getCurrentUser fails", async () => {
      vi.mocked(authService.isAuthenticated).mockReturnValue(true);
      vi.mocked(authService.getCurrentUser).mockResolvedValue(null);

      const { result } = renderHook(() => React.useContext(AuthContext), {
        wrapper,
      });

      await waitFor(() => {
        expect(result.current?.isLoading).toBe(false);
      });

      expect(result.current?.user).toBeNull();
      expect(result.current?.isAuthenticated).toBe(false);
      expect(authService.clearTokens).toHaveBeenCalled();
    });

    it("should handle errors during initialization", async () => {
      vi.mocked(authService.isAuthenticated).mockReturnValue(true);
      vi.mocked(authService.getCurrentUser).mockRejectedValue(
        new Error("Network error"),
      );

      const { result } = renderHook(() => React.useContext(AuthContext), {
        wrapper,
      });

      await waitFor(() => {
        expect(result.current?.isLoading).toBe(false);
      });

      expect(result.current?.user).toBeNull();
      expect(result.current?.isAuthenticated).toBe(false);
      expect(authService.clearTokens).toHaveBeenCalled();
    });

    it("should set user to null when not authenticated", async () => {
      vi.mocked(authService.isAuthenticated).mockReturnValue(false);

      const { result } = renderHook(() => React.useContext(AuthContext), {
        wrapper,
      });

      await waitFor(() => {
        expect(result.current?.isLoading).toBe(false);
      });

      expect(result.current?.user).toBeNull();
      expect(result.current?.isAuthenticated).toBe(false);
      expect(authService.getCurrentUser).not.toHaveBeenCalled();
    });
  });

  describe("Login Operations", () => {
    it("should update user state on successful teacher login", async () => {
      vi.mocked(authService.isAuthenticated).mockReturnValue(false);
      const mockUser = {
        id: 1,
        email: "teacher@example.com",
        role: "TEACHER" as const,
      };
      vi.mocked(authService.teacherLogin).mockResolvedValue({
        success: true,
        user: mockUser,
      });

      const { result } = renderHook(() => React.useContext(AuthContext), {
        wrapper,
      });

      await waitFor(() => {
        expect(result.current?.isLoading).toBe(false);
      });

      let loginResult: { success: boolean; user?: unknown } | undefined;
      await act(async () => {
        loginResult = await result.current?.teacherLogin(
          "teacher@example.com",
          "password123",
        );
      });

      expect(loginResult?.success).toBe(true);
      expect(result.current?.user).toEqual(mockUser);
      expect(result.current?.isAuthenticated).toBe(true);
    });

    it("should not update user state on failed login", async () => {
      vi.mocked(authService.isAuthenticated).mockReturnValue(false);
      vi.mocked(authService.teacherLogin).mockResolvedValue({
        success: false,
        message: "Invalid credentials",
      });

      const { result } = renderHook(() => React.useContext(AuthContext), {
        wrapper,
      });

      await waitFor(() => {
        expect(result.current?.isLoading).toBe(false);
      });

      let loginResult: { success: boolean; message?: string } | undefined;
      await act(async () => {
        loginResult = await result.current?.teacherLogin(
          "wrong@example.com",
          "wrong",
        );
      });

      expect(loginResult?.success).toBe(false);
      expect(result.current?.user).toBeNull();
      expect(result.current?.isAuthenticated).toBe(false);
    });

    it("should handle admin login", async () => {
      vi.mocked(authService.isAuthenticated).mockReturnValue(false);
      const mockUser = {
        id: 1,
        email: "admin@example.com",
        role: "ADMIN" as const,
      };
      vi.mocked(authService.adminLogin).mockResolvedValue({
        success: true,
        user: mockUser,
      });

      const { result } = renderHook(() => React.useContext(AuthContext), {
        wrapper,
      });

      await waitFor(() => {
        expect(result.current?.isLoading).toBe(false);
      });

      await act(async () => {
        await result.current?.login("admin@example.com", "password123");
      });

      expect(result.current?.user).toEqual(mockUser);
      expect(result.current?.isAuthenticated).toBe(true);
    });

    it("should handle guest login", async () => {
      vi.mocked(authService.isAuthenticated).mockReturnValue(false);
      const mockUser = {
        id: 0,
        email: "guest@example.com",
        role: "GUEST" as const,
      };
      vi.mocked(authService.guestLogin).mockResolvedValue({
        success: true,
        user: mockUser,
      });

      const { result } = renderHook(() => React.useContext(AuthContext), {
        wrapper,
      });

      await waitFor(() => {
        expect(result.current?.isLoading).toBe(false);
      });

      await act(async () => {
        await result.current?.guestLogin();
      });

      expect(result.current?.user).toEqual(mockUser);
      expect(result.current?.isAuthenticated).toBe(true);
    });

    it("should handle verification code login", async () => {
      vi.mocked(authService.isAuthenticated).mockReturnValue(false);
      const mockUser = {
        id: 1,
        email: "teacher@example.com",
        role: "TEACHER" as const,
      };
      vi.mocked(authService.verificationCodeLogin).mockResolvedValue({
        success: true,
        user: mockUser,
      });

      const { result } = renderHook(() => React.useContext(AuthContext), {
        wrapper,
      });

      await waitFor(() => {
        expect(result.current?.isLoading).toBe(false);
      });

      await act(async () => {
        await result.current?.verificationCodeLogin(
          "123456",
          "teacher@example.com",
        );
      });

      expect(result.current?.user).toEqual(mockUser);
      expect(result.current?.isAuthenticated).toBe(true);
    });
  });

  describe("Logout", () => {
    it("should clear user state on logout", async () => {
      const mockUser = {
        id: 1,
        email: "test@example.com",
        role: "TEACHER" as const,
      };
      vi.mocked(authService.isAuthenticated).mockReturnValue(true);
      vi.mocked(authService.getCurrentUser).mockResolvedValue(mockUser);
      vi.mocked(authService.logout).mockResolvedValue();

      const { result } = renderHook(() => React.useContext(AuthContext), {
        wrapper,
      });

      await waitFor(() => {
        expect(result.current?.user).toEqual(mockUser);
      });

      await act(async () => {
        await result.current?.logout();
      });

      expect(result.current?.user).toBeNull();
      expect(result.current?.isAuthenticated).toBe(false);
      expect(authService.logout).toHaveBeenCalled();
    });
  });

  describe("Refresh User", () => {
    it("should refresh user data when authenticated", async () => {
      const initialUser = {
        id: 1,
        email: "test@example.com",
        role: "TEACHER" as const,
      };
      const updatedUser = {
        id: 1,
        email: "test@example.com",
        role: "TEACHER" as const,
        first_name: "John",
      };

      vi.mocked(authService.isAuthenticated)
        .mockReturnValueOnce(true)
        .mockReturnValueOnce(true);
      vi.mocked(authService.getCurrentUser)
        .mockResolvedValueOnce(initialUser)
        .mockResolvedValueOnce(updatedUser);

      const { result } = renderHook(() => React.useContext(AuthContext), {
        wrapper,
      });

      await waitFor(() => {
        expect(result.current?.user).toEqual(initialUser);
      });

      await act(async () => {
        await result.current?.refreshUser();
      });

      expect(result.current?.user).toEqual(updatedUser);
    });

    it("should clear user when not authenticated during refresh", async () => {
      const mockUser = {
        id: 1,
        email: "test@example.com",
        role: "TEACHER" as const,
      };
      vi.mocked(authService.isAuthenticated)
        .mockReturnValueOnce(true)
        .mockReturnValueOnce(false);
      vi.mocked(authService.getCurrentUser).mockResolvedValue(mockUser);

      const { result } = renderHook(() => React.useContext(AuthContext), {
        wrapper,
      });

      await waitFor(() => {
        expect(result.current?.user).toEqual(mockUser);
      });

      await act(async () => {
        await result.current?.refreshUser();
      });

      expect(result.current?.user).toBeNull();
      expect(result.current?.isAuthenticated).toBe(false);
    });

    it("should clear tokens on refresh error", async () => {
      const mockUser = {
        id: 1,
        email: "test@example.com",
        role: "TEACHER" as const,
      };
      vi.mocked(authService.isAuthenticated)
        .mockReturnValueOnce(true)
        .mockReturnValueOnce(true);
      vi.mocked(authService.getCurrentUser)
        .mockResolvedValueOnce(mockUser)
        .mockRejectedValueOnce(new Error("Network error"));

      const { result } = renderHook(() => React.useContext(AuthContext), {
        wrapper,
      });

      await waitFor(() => {
        expect(result.current?.user).toEqual(mockUser);
      });

      await act(async () => {
        await result.current?.refreshUser();
      });

      expect(result.current?.user).toBeNull();
      expect(authService.clearTokens).toHaveBeenCalled();
    });
  });

  describe("Registration and Password Operations", () => {
    it("should forward request verification code to authService", async () => {
      vi.mocked(authService.isAuthenticated).mockReturnValue(false);
      vi.mocked(authService.requestVerificationCode).mockResolvedValue({
        success: true,
        message: "Code sent",
      });

      const { result } = renderHook(() => React.useContext(AuthContext), {
        wrapper,
      });

      await waitFor(() => {
        expect(result.current?.isLoading).toBe(false);
      });

      let requestResult: { success: boolean } | undefined;
      await act(async () => {
        requestResult =
          await result.current?.requestVerificationCode("test@example.com");
      });

      expect(requestResult?.success).toBe(true);
      expect(authService.requestVerificationCode).toHaveBeenCalledWith(
        "test@example.com",
      );
    });

    it("should forward register teacher to authService", async () => {
      vi.mocked(authService.isAuthenticated).mockReturnValue(false);
      vi.mocked(authService.registerTeacher).mockResolvedValue({
        success: true,
        message: "Teacher registered",
      });

      const { result } = renderHook(() => React.useContext(AuthContext), {
        wrapper,
      });

      await waitFor(() => {
        expect(result.current?.isLoading).toBe(false);
      });

      let registerResult: { success: boolean } | undefined;
      await act(async () => {
        registerResult = await result.current?.registerTeacher(
          "test@example.com",
          "John",
          "Doe",
        );
      });

      expect(registerResult?.success).toBe(true);
      expect(authService.registerTeacher).toHaveBeenCalledWith(
        "test@example.com",
        "John",
        "Doe",
      );
    });

    it("should forward reset password to authService", async () => {
      vi.mocked(authService.isAuthenticated).mockReturnValue(false);
      vi.mocked(authService.resetPassword).mockResolvedValue({
        success: true,
        message: "Password reset email sent",
      });

      const { result } = renderHook(() => React.useContext(AuthContext), {
        wrapper,
      });

      await waitFor(() => {
        expect(result.current?.isLoading).toBe(false);
      });

      let resetResult: { success: boolean } | undefined;
      await act(async () => {
        resetResult = await result.current?.resetPassword("test@example.com");
      });

      expect(resetResult?.success).toBe(true);
      expect(authService.resetPassword).toHaveBeenCalledWith(
        "test@example.com",
      );
    });
  });
});
