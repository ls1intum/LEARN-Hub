import { describe, it, expect, vi } from "vitest";
import { renderHook, act } from "@testing-library/react";
import { useForm } from "../useForm";

interface TestFormData {
  email: string;
  password: string;
  age: number;
}

describe("useForm", () => {
  const initialValues: TestFormData = {
    email: "",
    password: "",
    age: 0,
  };

  it("should initialize with correct values", () => {
    const { result } = renderHook(() =>
      useForm({
        initialValues,
        onSubmit: vi.fn(),
      }),
    );

    expect(result.current.values).toEqual(initialValues);
    expect(result.current.errors).toEqual({});
    expect(result.current.isSubmitting).toBe(false);
  });

  it("should update values when setValue is called", () => {
    const { result } = renderHook(() =>
      useForm({
        initialValues,
        onSubmit: vi.fn(),
      }),
    );

    act(() => {
      result.current.setValue("email", "test@example.com");
    });

    expect(result.current.values.email).toBe("test@example.com");
  });

  it("should clear error when field value is updated", () => {
    const { result } = renderHook(() =>
      useForm({
        initialValues,
        onSubmit: vi.fn(),
      }),
    );

    // Set an error first
    act(() => {
      result.current.setError("email", "Email is required");
    });

    expect(result.current.errors.email).toBe("Email is required");

    // Update the field value
    act(() => {
      result.current.setValue("email", "test@example.com");
    });

    expect(result.current.errors.email).toBe("");
  });

  it("should validate form before submission", async () => {
    const validate = vi.fn().mockReturnValue({
      email: "Email is required",
    });
    const onSubmit = vi.fn();

    const { result } = renderHook(() =>
      useForm({
        initialValues,
        onSubmit,
        validate,
      }),
    );

    await act(async () => {
      await result.current.handleSubmit();
    });

    expect(validate).toHaveBeenCalledWith(initialValues);
    expect(result.current.errors.email).toBe("Email is required");
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it("should call onSubmit when validation passes", async () => {
    const onSubmit = vi.fn();
    const { result } = renderHook(() =>
      useForm({
        initialValues,
        onSubmit,
      }),
    );

    await act(async () => {
      await result.current.handleSubmit();
    });

    expect(onSubmit).toHaveBeenCalledWith(initialValues);
  });

  it("should reset form to initial values", () => {
    const { result } = renderHook(() =>
      useForm({
        initialValues,
        onSubmit: vi.fn(),
      }),
    );

    // Update values and errors
    act(() => {
      result.current.setValue("email", "test@example.com");
      result.current.setError("password", "Password is required");
    });

    // Reset
    act(() => {
      result.current.reset();
    });

    expect(result.current.values).toEqual(initialValues);
    expect(result.current.errors).toEqual({});
    expect(result.current.isSubmitting).toBe(false);
  });

  it("should handle form submission", async () => {
    const onSubmit = vi.fn();
    const { result } = renderHook(() =>
      useForm({
        initialValues,
        onSubmit,
      }),
    );

    expect(result.current.isSubmitting).toBe(false);

    await act(async () => {
      await result.current.handleSubmit();
    });

    expect(onSubmit).toHaveBeenCalledWith(initialValues);
    expect(result.current.isSubmitting).toBe(false);
  });
});
