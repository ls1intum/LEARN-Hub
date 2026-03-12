/**
 * Simple secure token storage utility
 * Uses sessionStorage for automatic cleanup when browser session ends
 */

const STORAGE_PREFIX = "learn_hub_";

export const secureStorage = {
  getAccessToken(): string | null {
    try {
      return sessionStorage.getItem(`${STORAGE_PREFIX}accessToken`);
    } catch {
      return null;
    }
  },

  getRefreshToken(): string | null {
    try {
      return sessionStorage.getItem(`${STORAGE_PREFIX}refreshToken`);
    } catch {
      return null;
    }
  },

  setAccessToken(token: string): void {
    try {
      sessionStorage.setItem(`${STORAGE_PREFIX}accessToken`, token);
    } catch {
      // Ignore storage errors
    }
  },

  setRefreshToken(token: string): void {
    try {
      sessionStorage.setItem(`${STORAGE_PREFIX}refreshToken`, token);
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
      sessionStorage.removeItem(`${STORAGE_PREFIX}accessToken`);
      sessionStorage.removeItem(`${STORAGE_PREFIX}refreshToken`);
    } catch {
      // Ignore storage errors
    }
  },
};
