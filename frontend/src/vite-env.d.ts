/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_REGISTRY_BASE_URL?: string;
  readonly VITE_TELEMETRY_BASE_URL?: string;
  readonly VITE_ALERTS_BASE_URL?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
