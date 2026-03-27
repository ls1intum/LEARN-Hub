import { render } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { ActivityForm } from "../ActivityForm";

describe("ActivityForm", () => {
  it("defaults duration max to the same value as duration min", () => {
    const { container } = render(
      <ActivityForm onSubmit={vi.fn()} onCancel={vi.fn()} />,
    );

    expect(container.querySelector("#minimum-duration")).toHaveValue(15);
    expect(container.querySelector("#maximum-duration")).toHaveValue(15);
  });

  it("uses duration min as fallback when initial data has no duration max", () => {
    const { container } = render(
      <ActivityForm
        onSubmit={vi.fn()}
        onCancel={vi.fn()}
        initialData={{
          durationMinMinutes: 45,
        }}
      />,
    );

    expect(container.querySelector("#minimum-duration")).toHaveValue(45);
    expect(container.querySelector("#maximum-duration")).toHaveValue(45);
  });
});
