// Test-specific type definitions to replace any/unknown in tests

// Mock function types
// eslint-disable-next-line @typescript-eslint/no-explicit-any
export type MockFunction<T extends (...args: any[]) => any> = T & {
  mockImplementation: (fn: T) => MockFunction<T>;
  mockResolvedValue: (value: ReturnType<T>) => MockFunction<T>;
  mockRejectedValue: (error: Error) => MockFunction<T>;
  mockReturnValue: (value: ReturnType<T>) => MockFunction<T>;
  mockClear: () => void;
  mockReset: () => void;
  calls: Parameters<T>[];
  results: Array<{ type: "return" | "throw"; value: ReturnType<T> | Error }>;
};

// Mock response types
export interface MockResponse {
  ok: boolean;
  status: number;
  statusText: string;
  headers: Headers;
  json: () => Promise<unknown>;
  text: () => Promise<string>;
  blob: () => Promise<Blob>;
  arrayBuffer: () => Promise<ArrayBuffer>;
  formData: () => Promise<FormData>;
}

// Mock fetch function
export type MockFetch = MockFunction<typeof fetch>;

// Test utility types
export interface TestUtils {
  describe: typeof describe;
  it: typeof it;
  beforeEach: typeof beforeEach;
  afterEach: typeof afterEach;
  expect: typeof expect;
  vi: typeof vi;
}

// Global test environment
export interface TestGlobal extends Window {
  describe: typeof describe;
  it: typeof it;
  beforeEach: typeof beforeEach;
  afterEach: typeof afterEach;
  expect: typeof expect;
  vi: typeof vi;
}

// Mock router types
export interface MockRouter {
  navigate: MockFunction<(path: string) => void>;
  location: {
    pathname: string;
    search: string;
    hash: string;
    state: unknown;
  };
}

// Mock API response types
export interface MockApiResponse<T = unknown> {
  success: boolean;
  data?: T;
  message?: string;
  error?: string;
}

// Test data factory types
export interface TestDataFactory<T> {
  create: (overrides?: Partial<T>) => T;
  createMany: (count: number, overrides?: Partial<T>) => T[];
}
