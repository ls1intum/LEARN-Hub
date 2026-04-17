export interface ActivityNavigationState {
  backTo?: string;
  restoreScrollY?: number;
}

const ACTIVITY_ROUTE_PATTERNS = [
  /^\/activity-details\/[^/]+$/,
  /^\/activity-edit\/[^/]+$/,
];

export const isActivityRoute = (path?: string): boolean =>
  typeof path === "string" &&
  ACTIVITY_ROUTE_PATTERNS.some((pattern) => pattern.test(path));

export const getActivityBackTarget = (
  ...paths: Array<string | undefined>
): string | undefined => paths.find((path) => path && !isActivityRoute(path));
