import { createHttpClient } from './http';

export const services = {
  registry: createHttpClient(import.meta.env.VITE_REGISTRY_BASE_URL || '/registry'),
  telemetry: createHttpClient(import.meta.env.VITE_TELEMETRY_BASE_URL || '/telemetry'),
  alerts: createHttpClient(import.meta.env.VITE_ALERTS_BASE_URL || '/alerts'),
  identity: createHttpClient(import.meta.env.VITE_IDENTITY_BASE_URL || '/identity'),
};

export type Health = { status: string };
