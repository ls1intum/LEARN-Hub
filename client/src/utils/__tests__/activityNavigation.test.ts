import { describe, expect, it } from "vitest";
import {
  getActivityBackTarget,
  isActivityRoute,
} from "../activityNavigation";

describe("activityNavigation", () => {
  it("detects detail and edit routes", () => {
    expect(isActivityRoute("/activity-details/123")).toBe(true);
    expect(isActivityRoute("/activity-edit/123")).toBe(true);
    expect(isActivityRoute("/library")).toBe(false);
  });

  it("returns the first non-activity route", () => {
    expect(
      getActivityBackTarget(
        "/activity-edit/123",
        "/activity-details/123",
        "/library?page=2",
      ),
    ).toBe("/library?page=2");
  });

  it("returns undefined when all candidates are activity routes", () => {
    expect(
      getActivityBackTarget("/activity-edit/123", "/activity-details/123"),
    ).toBeUndefined();
  });
});
