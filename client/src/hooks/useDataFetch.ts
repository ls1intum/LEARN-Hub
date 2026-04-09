import { useEffect, useCallback } from "react";
import { usePageState } from "./usePageState";

interface UseDataFetchOptions<T> {
  fetchFn: () => Promise<T>;
  enabled?: boolean;
  onSuccess?: (data: T) => void;
  onError?: (error: Error) => void;
}

export function useDataFetch<T>({
  fetchFn,
  enabled = true,
  onSuccess,
  onError,
}: UseDataFetchOptions<T>) {
  const { data, isLoading, error, execute, setData } = usePageState<T>();

  const refetch = useCallback(() => {
    if (enabled) {
      return execute(fetchFn, onSuccess);
    }
    return Promise.resolve(null);
  }, [enabled, execute, fetchFn, onSuccess]);

  useEffect(() => {
    if (enabled) {
      refetch().catch((err) => {
        onError?.(err instanceof Error ? err : new Error(String(err)));
      });
    }
  }, [enabled, refetch, onError]);

  return {
    data,
    isLoading,
    error,
    refetch,
    setData,
  };
}
