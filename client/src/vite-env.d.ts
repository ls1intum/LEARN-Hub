/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_SERVER?: string;
  readonly VITE_ENVIRONMENT?: "local" | "development" | "production";
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
