/**
 * Simple logging service for the application
 */

const isDevelopment = import.meta.env.DEV;

export const logger = {
  debug: (message: string, data?: unknown, source?: string) => {
    if (isDevelopment) {
      const prefix = source ? `[${source}]` : "";
      console.debug(`${prefix} ${message}`, data || "");
    }
  },

  info: (message: string, data?: unknown, source?: string) => {
    if (isDevelopment) {
      const prefix = source ? `[${source}]` : "";
      console.info(`${prefix} ${message}`, data || "");
    }
  },

  warn: (message: string, data?: unknown, source?: string) => {
    const prefix = source ? `[${source}]` : "";
    console.warn(`${prefix} ${message}`, data || "");
  },

  error: (message: string, data?: unknown, source?: string) => {
    const prefix = source ? `[${source}]` : "";
    console.error(`${prefix} ${message}`, data || "");
  },
};
