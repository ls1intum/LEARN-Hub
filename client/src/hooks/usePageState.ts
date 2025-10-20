import { useState, useCallback } from "react";

interface PageState<T> {
  data: T | null;
  isLoading: boolean;
  error: string | null;
}

interface UsePageStateReturn<T> extends PageState<T> {
  setData: (data: T | null) => void;
  setLoading: (loading: boolean) => void;
  setError: (error: string | null) => void;
  execute: <R>(
    asyncFn: () => Promise<R>,
    onSuccess?: (result: R) => void,
  ) => Promise<R | null>;
  reset: () => void;
}

export function usePageState<T = unknown>(
  initialData: T | null = null,
): UsePageStateReturn<T> {
  const [data, setData] = useState<T | null>(initialData);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const setLoading = useCallback((loading: boolean) => {
    setIsLoading(loading);
    if (loading) setError(null); // Clear error when starting new operation
  }, []);

  const execute = useCallback(
    async <R>(
      asyncFn: () => Promise<R>,
      onSuccess?: (result: R) => void,
    ): Promise<R | null> => {
      setLoading(true);
      try {
        const result = await asyncFn();
        setError(null);
        // Store result in page state so consumers can read it
        setData(result as unknown as T);
        onSuccess?.(result);
        return result;
      } catch (err) {
        const errorMessage =
          err instanceof Error ? err.message : "An unexpected error occurred";
        setError(errorMessage);
        return null;
      } finally {
        setLoading(false);
      }
    },
    [setLoading],
  );

  const reset = useCallback(() => {
    setData(null);
    setIsLoading(false);
    setError(null);
  }, []);

  return {
    data,
    isLoading,
    error,
    setData,
    setLoading,
    setError,
    execute,
    reset,
  };
}
