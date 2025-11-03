/**
 * Environment version utility
 * Displays the current environment version in the UI
 */

export type EnvironmentType = "local" | "development" | "production";

/**
 * Get the current environment type from Vite environment variable
 */
export const getEnvironmentType = (): EnvironmentType => {
  const env = import.meta.env.VITE_ENVIRONMENT?.toLowerCase();
  if (env === "local" || env === "development" || env === "production") {
    return env;
  }
  // Default to local if not set or invalid
  return "local";
};

/**
 * Get the display text for the current environment
 * - local: "Local"
 * - development: "Development"
 * - production: "Public Testing"
 */
export const getEnvironmentVersion = (): string => {
  const env = getEnvironmentType();
  switch (env) {
    case "local":
      return "Local";
    case "development":
      return "Development";
    case "production":
      return "Public Testing";
    default:
      return "Local";
  }
};

/**
 * Get the badge variant color for the current environment
 */
export const getEnvironmentBadgeVariant = (): "default" | "secondary" | "outline" => {
  const env = getEnvironmentType();
  switch (env) {
    case "local":
      return "secondary";
    case "development":
      return "default";
    case "production":
      return "outline";
    default:
      return "secondary";
  }
};
