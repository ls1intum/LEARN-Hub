import { useState, useEffect } from "react";
import { ApiService } from "@/services/apiService";

interface UseEnvironmentResult {
  environment: string | null;
  isLoading: boolean;
  error: string | null;
}

/**
 * Custom hook to fetch and manage the current environment from the API
 * Handles loading and error states
 * Environments: local, staging, production
 */
export const useEnvironment = (): UseEnvironmentResult => {
  const [environment, setEnvironment] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchEnvironment = async () => {
      try {
        setIsLoading(true);
        setError(null);
        const data = await ApiService.getEnvironment();
        setEnvironment(data.environment);
      } catch (err) {
        const errorMessage =
          err instanceof Error ? err.message : "Failed to fetch environment";
        setError(errorMessage);
        // Fallback to local if fetching fails
        setEnvironment("local");
      } finally {
        setIsLoading(false);
      }
    };

    fetchEnvironment();
  }, []);

  return { environment, isLoading, error };
};
