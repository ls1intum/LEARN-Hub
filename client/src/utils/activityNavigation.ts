export interface ActivityNavigationState {
  backTo?: string;
  restoreScrollY?: number;
  /** Full path to return to the activity detail page (e.g., /library/123) */
  detailPath?: string;
}

const ACTIVITY_ROUTE_PATTERNS = [
  /^\/library\/[^/]+(\/edit)?$/,
  /^\/recommendations\/[^/]+(\/edit)?$/,
  /^\/favourites\/[^/]+(\/edit)?$/,
  /^\/drafts\/[^/]+(\/edit)?$/,
];

export const isActivityRoute = (path?: string): boolean =>
  typeof path === "string" &&
  ACTIVITY_ROUTE_PATTERNS.some((pattern) => pattern.test(path));

export const getActivityBackTarget = (
  ...paths: Array<string | undefined>
): string | undefined => paths.find((path) => path && !isActivityRoute(path));
