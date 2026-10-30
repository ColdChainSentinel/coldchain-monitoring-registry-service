import type { Measurement, MonitoringPoint, ThermalAlert } from '../types/domain';

// Explicit test-only fixtures. Production components never import this module.
export const point: MonitoringPoint = {
  id: 'b60d6bd0-e058-425d-9102-434b546e1284',
  code: 'CAM-01',
  name: 'Cámara de pruebas',
  type: 'ALMACENAMIENTO',
};
export const otherPoint: MonitoringPoint = {
  id: 'a8362663-11ad-42fe-b97f-f98cadf361a9',
  code: 'TR-02',
  name: 'Transporte de pruebas',
  type: 'TRANSPORTE',
};
export const measurement: Measurement = {
  id: '0a41469b-9a1d-4245-a360-29986b37f44b',
  monitoringPointId: point.id,
  temperature: 9,
  timestamp: '2026-10-29T10:00:00Z',
  evaluationStatus: 'COMPLETED',
};
export const alert: ThermalAlert = {
  id: 'c6423de0-8fae-4f15-bd77-a3e5b87ab9d6',
  measurementId: measurement.id,
  monitoringPointId: point.id,
  temperature: 9,
  thresholdMin: 2,
  thresholdMax: 8,
  status: 'OPEN',
  type: 'HIGH_TEMPERATURE',
  createdAt: '2026-10-29T10:00:01Z',
};
export function json(value: unknown, status = 200) {
  return new Response(JSON.stringify(value), { status, headers: { 'Content-Type': 'application/json' } });
}
