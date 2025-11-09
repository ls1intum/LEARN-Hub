/**
 * Environment configuration utility
 * Maps environment values to display text and badge styles
 */

export type EnvironmentType = "local" | "staging" | "production";

interface EnvironmentConfig {
  displayText: string;
  badgeVariant: "default" | "secondary" | "outline";
}

/**
 * Configuration mapping for each environment
 * - local: Displays "Local" with secondary styling
 * - staging: Displays "Staging" with default styling
 * - production: Displays "Public Testing" with outline styling
 */
const ENVIRONMENT_CONFIG: Record<EnvironmentType, EnvironmentConfig> = {
  local: {
    displayText: "Local",
    badgeVariant: "secondary",
  },
  staging: {
    displayText: "Staging",
    badgeVariant: "default",
  },
  production: {
    displayText: "Public Testing",
    badgeVariant: "outline",
  },
};

/**
 * Get the display text for an environment
 */
export const getEnvironmentDisplayText = (environment: string): string => {
  const env = environment.toLowerCase() as EnvironmentType;
  return ENVIRONMENT_CONFIG[env]?.displayText || "Unknown";
};

/**
 * Get the badge variant for an environment
 */
export const getEnvironmentBadgeVariant = (
  environment: string,
): "default" | "secondary" | "outline" => {
  const env = environment.toLowerCase() as EnvironmentType;
  return ENVIRONMENT_CONFIG[env]?.badgeVariant || "secondary";
};
