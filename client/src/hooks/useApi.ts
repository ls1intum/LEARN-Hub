import { useCallback } from "react";
import { usePageState } from "./usePageState";

interface UseApiOptions<T> {
  onSuccess?: (data: T) => void;
}

export function useApi<T = unknown>({ onSuccess }: UseApiOptions<T> = {}) {
  const { data, isLoading, error, execute } = usePageState<T>();

  const call = useCallback(
    async <R>(
      apiCall: () => Promise<R>,
      successCallback?: (result: R) => void,
    ): Promise<R | null> => {
      return execute(apiCall, (result) => {
        onSuccess?.((result as unknown) as T);
        successCallback?.(result);
      });
    },
    [execute, onSuccess],
  );

  return {
    data,
    isLoading,
    error,
    call,
  };
}
