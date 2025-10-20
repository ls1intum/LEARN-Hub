/**
 * Simple secure token storage utility
 * Uses sessionStorage for automatic cleanup when browser session ends
 */

const STORAGE_PREFIX = "learn_hub_";

export const secureStorage = {
  getAccessToken(): string | null {
    try {
      return sessionStorage.getItem(`${STORAGE_PREFIX}access_token`);
    } catch {
      return null;
    }
  },

  getRefreshToken(): string | null {
    try {
      return sessionStorage.getItem(`${STORAGE_PREFIX}refresh_token`);
    } catch {
      return null;
    }
  },

  setAccessToken(token: string): void {
    try {
      sessionStorage.setItem(`${STORAGE_PREFIX}access_token`, token);
    } catch {
      // Ignore storage errors
    }
  },

  setRefreshToken(token: string): void {
    try {
      sessionStorage.setItem(`${STORAGE_PREFIX}refresh_token`, token);
    } catch {
      // Ignore storage errors
    }
  },

  setTokens(accessToken: string, refreshToken: string): void {
    this.setAccessToken(accessToken);
    this.setRefreshToken(refreshToken);
  },

  clearTokens(): void {
    try {
      sessionStorage.removeItem(`${STORAGE_PREFIX}access_token`);
      sessionStorage.removeItem(`${STORAGE_PREFIX}refresh_token`);
    } catch {
      // Ignore storage errors
    }
  },
};
