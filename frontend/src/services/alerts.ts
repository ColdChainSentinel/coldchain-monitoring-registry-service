import { services } from '../api/services';
import { readAlerts } from '../lib/contracts';
import type { ConfigureThermalRule, ThermalRule } from '../types/domain';

// Read-only UI for alert history. Thermal rules can be configured per point (HU-09).
export const alertsApi = {
  list: async (pointId: string, signal?: AbortSignal) =>
    readAlerts(
      await services.alerts.get<unknown>(
        `/api/alerts?monitoringPointId=${encodeURIComponent(pointId)}`,
        signal,
      ),
    ),
  getRule: (pointId: string, signal?: AbortSignal) =>
    services.alerts.get<ThermalRule>(`/api/alerts/rules/${encodeURIComponent(pointId)}`, signal),
  configureRule: (pointId: string, rule: ConfigureThermalRule) =>
    services.alerts.put<ThermalRule>(`/api/alerts/rules/${encodeURIComponent(pointId)}`, rule),
};
