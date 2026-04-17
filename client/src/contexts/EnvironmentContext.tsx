import React, { createContext, useContext, useEffect, useState } from "react";
import { ApiService } from "@/services/apiService";

interface EnvironmentContextValue {
  environment: string | null;
  isLoading: boolean;
  error: string | null;
}

const EnvironmentContext = createContext<EnvironmentContextValue>({
  environment: null,
  isLoading: true,
  error: null,
});

export const EnvironmentProvider: React.FC<{ children: React.ReactNode }> = ({
  children,
}) => {
  const [environment, setEnvironment] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchEnvironment = async () => {
      try {
        const data = await ApiService.getEnvironment();
        setEnvironment(data.environment);
      } catch (err) {
        setError(
          err instanceof Error ? err.message : "Failed to fetch environment",
        );
        setEnvironment("local");
      } finally {
        setIsLoading(false);
      }
    };

    fetchEnvironment();
  }, []);

  return (
    <EnvironmentContext.Provider value={{ environment, isLoading, error }}>
      {children}
    </EnvironmentContext.Provider>
  );
};

export const useEnvironmentContext = () => useContext(EnvironmentContext);
