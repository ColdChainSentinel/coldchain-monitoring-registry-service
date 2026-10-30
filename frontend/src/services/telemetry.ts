import { services } from '../api/services';
import { readMeasurement, readMeasurements } from '../lib/contracts';
import type { CreateMeasurement } from '../types/domain';

export const telemetryApi = {
  list: async (pointId: string, signal?: AbortSignal) =>
    readMeasurements(
      await services.telemetry.get<unknown>(
        `/api/measurements?monitoringPointId=${encodeURIComponent(pointId)}`,
        signal,
      ),
    ),
  create: async (measurement: CreateMeasurement) =>
    readMeasurement(await services.telemetry.post<unknown>('/api/measurements', measurement)),
};
