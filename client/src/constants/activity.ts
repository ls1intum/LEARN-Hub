export const ACTIVITY_CONSTANTS = {
  AGE_RANGE: {
    MIN: 6,
    MAX: 15,
  },
  DURATION_RANGE: {
    MIN: 15,
    MAX: 240,
    STEP: 5,
  },
  PREP_CLEANUP_RANGE: {
    MIN: 0,
    MAX: 30,
    STEP: 1,
  },
  ENERGY_LEVELS: ["low", "medium", "high"] as const,
  ITEMS_PER_PAGE: 20,
  MAX_PDF_SIZE: "10MB",
} as const;

export const ROUTES = {
  LOGIN: "/login",
  RECOMMENDATIONS: "/recommendations",
  LIBRARY: "/library",
  UPLOAD: "/upload",
  USERS: "/users",
  RESULTS: "/results",
  ACTIVITY_DETAILS: "/activity-details",
  // Legacy routes for backward compatibility
  LEGACY_DASHBOARD: "/dashboard",
  LEGACY_FORM: "/recommendation-form",
  LEGACY_ADD_ACTIVITY: "/add-activity",
  LEGACY_FAVORITES: "/favorites",
  LEGACY_ADMIN_USERS: "/admin/users",
  LEGACY_ADMIN_ACTIVITIES: "/admin/activities",
} as const;
