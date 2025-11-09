import { describe, it, expect, vi, beforeEach } from "vitest";
import { renderHook, waitFor, act } from "@testing-library/react";
import { useApi } from "../useApi";

describe("useApi", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("should initialize with null data and no loading/error state", () => {
    const { result } = renderHook(() => useApi());

    expect(result.current.data).toBeNull();
    expect(result.current.isLoading).toBe(false);
    expect(result.current.error).toBeNull();
  });

  it("should handle successful API call", async () => {
    const { result } = renderHook(() => useApi());
    const mockApiCall = vi.fn().mockResolvedValue({ id: 1, name: "Test" });

    await act(async () => {
      await result.current.call(mockApiCall);
    });

    expect(result.current.data).toEqual({ id: 1, name: "Test" });
    expect(result.current.isLoading).toBe(false);
    expect(result.current.error).toBeNull();
    expect(mockApiCall).toHaveBeenCalledTimes(1);
  });

  it("should handle API call failure", async () => {
    const { result } = renderHook(() => useApi());
    const mockApiCall = vi.fn().mockRejectedValue(new Error("API Error"));

    await act(async () => {
      await result.current.call(mockApiCall);
    });

    expect(result.current.data).toBeNull();
    expect(result.current.isLoading).toBe(false);
    expect(result.current.error).toBe("API Error");
    expect(mockApiCall).toHaveBeenCalledTimes(1);
  });

  it("should set loading state during API call", async () => {
    const { result } = renderHook(() => useApi());
    let resolvePromise: (value: unknown) => void;
    const mockApiCall = vi.fn().mockReturnValue(
      new Promise((resolve) => {
        resolvePromise = resolve;
      }),
    );

    // Start the call but don't await it yet
    let callPromise: Promise<unknown>;
    act(() => {
      callPromise = result.current.call(mockApiCall);
    });

    // Check loading state is true during call
    await waitFor(() => {
      expect(result.current.isLoading).toBe(true);
    });

    // Resolve the promise and wait for completion
    await act(async () => {
      resolvePromise!({ id: 1 });
      await callPromise!;
    });

    expect(result.current.isLoading).toBe(false);
  });

  it("should call onSuccess callback after successful API call", async () => {
    const onSuccess = vi.fn();
    const { result } = renderHook(() => useApi({ onSuccess }));
    const mockApiCall = vi.fn().mockResolvedValue({ id: 1, name: "Test" });

    await act(async () => {
      await result.current.call(mockApiCall);
    });

    expect(onSuccess).toHaveBeenCalledWith({ id: 1, name: "Test" });
  });

  it("should call success callback parameter", async () => {
    const successCallback = vi.fn();
    const { result } = renderHook(() => useApi());
    const mockApiCall = vi.fn().mockResolvedValue({ id: 1, name: "Test" });

    await act(async () => {
      await result.current.call(mockApiCall, successCallback);
    });

    expect(successCallback).toHaveBeenCalledWith({ id: 1, name: "Test" });
  });

  it("should call both onSuccess and successCallback", async () => {
    const onSuccess = vi.fn();
    const successCallback = vi.fn();
    const { result } = renderHook(() => useApi({ onSuccess }));
    const mockApiCall = vi.fn().mockResolvedValue({ id: 1, name: "Test" });

    await act(async () => {
      await result.current.call(mockApiCall, successCallback);
    });

    expect(onSuccess).toHaveBeenCalledWith({ id: 1, name: "Test" });
    expect(successCallback).toHaveBeenCalledWith({ id: 1, name: "Test" });
  });

  it("should clear previous error on successful call", async () => {
    const { result } = renderHook(() => useApi());

    // First call that fails
    const failingCall = vi.fn().mockRejectedValue(new Error("API Error"));
    await act(async () => {
      await result.current.call(failingCall);
    });

    expect(result.current.error).toBe("API Error");

    // Second call that succeeds
    const successCall = vi.fn().mockResolvedValue({ id: 1 });
    await act(async () => {
      await result.current.call(successCall);
    });

    expect(result.current.error).toBeNull();
    expect(result.current.data).toEqual({ id: 1 });
  });

  it("should return result from call", async () => {
    const { result } = renderHook(() => useApi());
    const mockApiCall = vi.fn().mockResolvedValue({ id: 1, name: "Test" });

    let returnValue;
    await act(async () => {
      returnValue = await result.current.call(mockApiCall);
    });

    expect(returnValue).toEqual({ id: 1, name: "Test" });
  });

  it("should return null on failed call", async () => {
    const { result } = renderHook(() => useApi());
    const mockApiCall = vi.fn().mockRejectedValue(new Error("API Error"));

    let returnValue;
    await act(async () => {
      returnValue = await result.current.call(mockApiCall);
    });

    expect(returnValue).toBeNull();
  });
});
