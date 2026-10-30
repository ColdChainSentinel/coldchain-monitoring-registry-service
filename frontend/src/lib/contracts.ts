import { ResponseError } from '../api/http';
import { ExactDecimal } from './decimal';
import type { Measurement, MonitoringPoint, ThermalAlert } from '../types/domain';

const uuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
const record = (value: unknown): value is Record<string, unknown> => !!value && typeof value === 'object';
const id = (value: unknown) => typeof value === 'string' && uuid.test(value);
const text = (value: unknown) => typeof value === 'string' && value.length > 0;
const number = (value: unknown) =>
  (typeof value === 'number' && Number.isFinite(value)) || value instanceof ExactDecimal;
const date = (value: unknown) => typeof value === 'string' && Number.isFinite(Date.parse(value));

function read<T>(value: unknown, valid: (row: Record<string, unknown>) => boolean): T {
  if (!record(value) || !valid(value)) throw new ResponseError();
  return value as T;
}
function list<T>(value: unknown, parse: (row: unknown) => T): T[] {
  if (!Array.isArray(value)) throw new ResponseError();
  return value.map(parse);
}
export const readPoint = (value: unknown) =>
  read<MonitoringPoint>(
    value,
    (row) =>
      id(row.id) &&
      text(row.code) &&
      text(row.name) &&
      ['ALMACENAMIENTO', 'TRANSPORTE'].includes(String(row.type)),
  );
export const readPoints = (value: unknown) => list(value, readPoint);
export const readMeasurement = (value: unknown) =>
  read<Measurement>(
    value,
    (row) =>
      id(row.id) &&
      id(row.monitoringPointId) &&
      number(row.temperature) &&
      date(row.timestamp) &&
      ['COMPLETED', 'PENDING', 'RULE_NOT_CONFIGURED'].includes(String(row.evaluationStatus)),
  );
export const readMeasurements = (value: unknown) => list(value, readMeasurement);
export const readAlert = (value: unknown) =>
  read<ThermalAlert>(
    value,
    (row) =>
      id(row.id) &&
      id(row.measurementId) &&
      id(row.monitoringPointId) &&
      number(row.temperature) &&
      number(row.thresholdMin) &&
      number(row.thresholdMax) &&
      text(row.status) &&
      date(row.createdAt) &&
      (row.type === undefined || ['LOW_TEMPERATURE', 'HIGH_TEMPERATURE'].includes(String(row.type))),
  );
export const readAlerts = (value: unknown) => list(value, readAlert);
