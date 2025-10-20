import { useState, useEffect } from "react";
import { apiService } from "@/services/apiService";
import { fieldValues as defaultFieldValues } from "@/constants/fieldValues";
import { logger } from "@/services/logger";

interface FieldValues {
  format: string[];
  resources_available: string[];
  bloom_level: string[];
  topics: string[];
  mental_load: string[];
  physical_energy: string[];
  priority_categories: string[];
  age_range: { min: number; max: number };
}

export const useFieldValues = () => {
  const [fieldValues, setFieldValues] =
    useState<FieldValues>(defaultFieldValues);
  const [isLoading, setIsLoading] = useState(false); // Start with false since we have defaults
  const [error, setError] = useState<string | null>(null);

  const fetchFieldValues = async () => {
    try {
      setIsLoading(true);
      setError(null);

      const response = await apiService.getFieldValues();
      if (response) {
        // Merge API response with defaults to ensure all fields are present
        setFieldValues({
          ...defaultFieldValues,
          ...response,
        });
      } else {
        // Fall back to default values if API fails
        logger.warn(
          "Failed to fetch field values from server, using defaults",
          null,
          "useFieldValues",
        );
        setFieldValues(defaultFieldValues);
      }
    } catch (err) {
      logger.error("Error fetching field values", err, "useFieldValues");
      setError(
        err instanceof Error ? err.message : "Failed to fetch field values",
      );
      // Fall back to default values
      setFieldValues(defaultFieldValues);
    } finally {
      setIsLoading(false);
    }
  };

  // For first-party client, we primarily use hardcoded values
  // API fetch is optional and mainly for third-party clients
  useEffect(() => {
    // Only fetch from API if explicitly needed
    // For now, we'll skip the API call and use hardcoded values
    // fetchFieldValues();
  }, []);

  return {
    fieldValues,
    isLoading,
    error,
    refetch: fetchFieldValues,
  };
};
