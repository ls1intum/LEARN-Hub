import type { Location } from "react-router-dom";

const DEFAULT_POST_LOGIN_PATH = "/recommendations";

type RedirectLocation = Pick<Location, "pathname" | "search" | "hash">;

export interface AuthRedirectState {
  from?: RedirectLocation;
  message?: string;
}

export const getLoginRedirectState = (
  location: RedirectLocation,
): AuthRedirectState => ({
  from: {
    pathname: location.pathname,
    search: location.search,
    hash: location.hash,
  },
});

export const getPostLoginRedirectPath = (
  state?: AuthRedirectState | null,
): string => {
  const from = state?.from;

  if (!from?.pathname) {
    return DEFAULT_POST_LOGIN_PATH;
  }

  if (from.pathname === "/" || from.pathname === "/login") {
    return DEFAULT_POST_LOGIN_PATH;
  }

  if (from.pathname.startsWith("/auth/verify")) {
    return DEFAULT_POST_LOGIN_PATH;
  }

  return `${from.pathname}${from.search ?? ""}${from.hash ?? ""}`;
};
