import { describe, expect, it } from "vitest";
import {
  getLoginRedirectState,
  getPostLoginRedirectPath,
} from "../authRedirect";

describe("authRedirect", () => {
  it("stores the full current location for a login redirect", () => {
    expect(
      getLoginRedirectState({
        pathname: "/library",
        search: "?page=2",
        hash: "#filters",
      }),
    ).toEqual({
      from: {
        pathname: "/library",
        search: "?page=2",
        hash: "#filters",
      },
    });
  });

  it("falls back to recommendations when no prior location exists", () => {
    expect(getPostLoginRedirectPath()).toBe("/recommendations");
  });

  it("returns users to their previous page", () => {
    expect(
      getPostLoginRedirectPath({
        from: {
          pathname: "/library",
          search: "?page=2",
          hash: "#filters",
        },
      }),
    ).toBe("/library?page=2#filters");
  });

  it("redirects landing-page logins to recommendations", () => {
    expect(
      getPostLoginRedirectPath({
        from: {
          pathname: "/",
          search: "",
          hash: "",
        },
      }),
    ).toBe("/recommendations");
  });

  it("does not bounce back to auth routes after login", () => {
    expect(
      getPostLoginRedirectPath({
        from: {
          pathname: "/login",
          search: "",
          hash: "",
        },
      }),
    ).toBe("/recommendations");

    expect(
      getPostLoginRedirectPath({
        from: {
          pathname: "/auth/verify",
          search: "?code=123456",
          hash: "",
        },
      }),
    ).toBe("/recommendations");
  });
});
