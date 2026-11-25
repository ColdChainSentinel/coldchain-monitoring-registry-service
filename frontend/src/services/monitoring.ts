import { services } from '../api/services';
import { readPoint, readPoints } from '../lib/contracts';
import type { CreateDevice, CreatePoint, Device } from '../types/domain';

export const monitoringApi = {
  list: async (signal?: AbortSignal) =>
    readPoints(await services.registry.get<unknown>('/api/v1/monitoring-points', signal)),
  create: async (point: CreatePoint) =>
    readPoint(await services.registry.post<unknown>('/api/v1/monitoring-points', point)),
  listDevices: (pointId: string, signal?: AbortSignal) =>
    services.registry.get<Device[]>(
      `/api/v1/devices?monitoringPointId=${encodeURIComponent(pointId)}`,
      signal,
    ),
  createDevice: (device: CreateDevice) =>
    services.registry.post<Device>('/api/v1/devices', device),
};
