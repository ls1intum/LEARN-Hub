import "@testing-library/jest-dom";
import {
  vi,
  describe,
  it,
  beforeEach,
  afterEach,
  expect,
  beforeAll,
  afterAll,
} from "vitest";
import React from "react";

// Mock storage BEFORE importing MSW (MSW uses localStorage on init)
const sessionStorageMock = {
  getItem: vi.fn(() => null),
  setItem: vi.fn(),
  removeItem: vi.fn(),
  clear: vi.fn(),
  length: 0,
  key: vi.fn(() => null),
};

const localStorageMock = {
  getItem: vi.fn(() => null),
  setItem: vi.fn(),
  removeItem: vi.fn(),
  clear: vi.fn(),
  length: 0,
  key: vi.fn(() => null),
};

// Set up storage mocks immediately for all global contexts
Object.defineProperty(globalThis, "sessionStorage", {
  value: sessionStorageMock,
  writable: true,
  configurable: true,
});

Object.defineProperty(globalThis, "localStorage", {
  value: localStorageMock,
  writable: true,
  configurable: true,
});

// Note: MSW server is initialized in individual test files to avoid initialization issues

// Mock heavy components to improve test performance
vi.mock("@/components/ui/LoadingState", () => ({
  LoadingState: ({ children }: { children: React.ReactNode }) =>
    React.createElement("div", { "data-testid": "loading-state" }, children),
  SkeletonGrid: () =>
    React.createElement(
      "div",
      { "data-testid": "skeleton-grid" },
      "Loading...",
    ),
}));

vi.mock("@/components/ui/ErrorDisplay", () => ({
  ErrorDisplay: ({ error }: { error: string }) =>
    React.createElement("div", { "data-testid": "error-display" }, error),
}));

// Mock complex components that cause performance issues
vi.mock("@/components/favourites/ActivityFavouritesTab", () => ({
  ActivityFavouritesTab: () =>
    React.createElement(
      "div",
      { "data-testid": "activity-favourites-tab" },
      "Activity Favourites Tab",
    ),
}));

vi.mock("@/components/favourites/LessonPlanFavouritesTab", () => ({
  LessonPlanFavouritesTab: () =>
    React.createElement(
      "div",
      { "data-testid": "lesson-plan-favourites-tab" },
      "Lesson Plan Favourites Tab",
    ),
}));

// Mock useAuth hook globally to prevent undefined errors
vi.mock("@/hooks/useAuth", () => ({
  useAuth: () => ({
    user: { id: 1, email: "test@example.com", role: "TEACHER" },
    isAuthenticated: true,
    isLoading: false,
    login: vi.fn(),
    teacherLogin: vi.fn(),
    verificationCodeLogin: vi.fn(),
    requestVerificationCode: vi.fn(),
    registerTeacher: vi.fn(),
    resetPassword: vi.fn(),
    guestLogin: vi.fn(),
    logout: vi.fn(),
    refreshUser: vi.fn(),
  }),
}));

// Mock useFieldValues hook
vi.mock("@/hooks/useFieldValues", () => ({
  useFieldValues: () => ({
    fieldValues: {
      format: ["unplugged", "digital", "hybrid"],
      resources_available: ["computers", "tablets", "handouts"],
      bloom_level: ["remember", "understand", "apply"],
      topics: ["decomposition", "patterns", "abstraction"],
      mental_load: ["low", "medium", "high"],
      physical_energy: ["low", "medium", "high"],
    },
  }),
}));

// Mock window.location
Object.defineProperty(window, "location", {
  value: {
    href: "http://localhost:3000",
    origin: "http://localhost:3000",
    pathname: "/",
    search: "",
    hash: "",
  },
  writable: true,
});

// Mock ResizeObserver
global.ResizeObserver = vi.fn().mockImplementation(() => ({
  observe: vi.fn(),
  unobserve: vi.fn(),
  disconnect: vi.fn(),
}));

// Mock IntersectionObserver
global.IntersectionObserver = vi.fn().mockImplementation(() => ({
  observe: vi.fn(),
  unobserve: vi.fn(),
  disconnect: vi.fn(),
}));

// Mock window.matchMedia
Object.defineProperty(window, "matchMedia", {
  writable: true,
  value: vi.fn().mockImplementation((query) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: vi.fn(), // deprecated
    removeListener: vi.fn(), // deprecated
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    dispatchEvent: vi.fn(),
  })),
});

// Extend globalThis with test functions
declare global {
  var describe: typeof import("vitest").describe;
  var it: typeof import("vitest").it;
  var beforeEach: typeof import("vitest").beforeEach;
  var afterEach: typeof import("vitest").afterEach;
  var beforeAll: typeof import("vitest").beforeAll;
  var afterAll: typeof import("vitest").afterAll;
  var expect: typeof import("vitest").expect;
  var vi: typeof import("vitest").vi;
}

// Assign test functions to global scope
(globalThis as unknown as Record<string, unknown>).describe = describe;
(globalThis as unknown as Record<string, unknown>).it = it;
(globalThis as unknown as Record<string, unknown>).beforeEach = beforeEach;
(globalThis as unknown as Record<string, unknown>).afterEach = afterEach;
(globalThis as unknown as Record<string, unknown>).beforeAll = beforeAll;
(globalThis as unknown as Record<string, unknown>).afterAll = afterAll;
(globalThis as unknown as Record<string, unknown>).expect = expect;
(globalThis as unknown as Record<string, unknown>).vi = vi;
