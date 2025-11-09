import { setupServer } from "msw/node";
import { handlers } from "./handlers";

/**
 * Mock server for Node.js test environment
 * This server intercepts HTTP requests during tests and returns mock responses
 */
export const server = setupServer(...handlers);
