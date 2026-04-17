import { useEnvironmentContext } from "@/contexts/EnvironmentContext";

/**
 * Returns the current environment from the app-level EnvironmentContext.
 * The environment is fetched once on initial load and never re-fetched.
 * Environments: local, staging, production
 */
export const useEnvironment = () => useEnvironmentContext();
