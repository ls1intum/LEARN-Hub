import { describe, expect, it } from "vitest";
import { getActivityBackTarget, isActivityRoute } from "../activityNavigation";

describe("activityNavigation", () => {
  it("detects detail and edit routes", () => {
    expect(isActivityRoute("/library/123")).toBe(true);
    expect(isActivityRoute("/library/123/edit")).toBe(true);
    expect(isActivityRoute("/recommendations/123")).toBe(true);
    expect(isActivityRoute("/recommendations/123/edit")).toBe(true);
    expect(isActivityRoute("/favourites/123")).toBe(true);
    expect(isActivityRoute("/favourites/123/edit")).toBe(true);
    expect(isActivityRoute("/drafts/123")).toBe(true);
    expect(isActivityRoute("/drafts/123/edit")).toBe(true);
    expect(isActivityRoute("/library")).toBe(false);
    expect(isActivityRoute("/recommendations")).toBe(false);
  });

  it("returns the first non-activity route", () => {
    expect(
      getActivityBackTarget(
        "/library/123/edit",
        "/library/123",
        "/library?page=2",
      ),
    ).toBe("/library?page=2");
  });

  it("returns undefined when all candidates are activity routes", () => {
    expect(
      getActivityBackTarget("/library/123/edit", "/library/123"),
    ).toBeUndefined();
  });
});
