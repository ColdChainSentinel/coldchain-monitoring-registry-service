import { services } from '../api/services';
import { readAlerts } from '../lib/contracts';

// Read-only UI. Never POST evaluate just to find out a measurement's outcome.
export const alertsApi = {
  list: async (pointId: string, signal?: AbortSignal) =>
    readAlerts(
      await services.alerts.get<unknown>(
        `/api/alerts?monitoringPointId=${encodeURIComponent(pointId)}`,
        signal,
      ),
    ),
};
