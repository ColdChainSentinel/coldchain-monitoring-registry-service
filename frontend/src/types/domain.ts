import type { Decimal } from '../lib/decimal';

export type PointType = 'ALMACENAMIENTO' | 'TRANSPORTE';
export interface MonitoringPoint {
  id: string;
  code: string;
  name: string;
  type: PointType;
}
export type CreatePoint = Omit<MonitoringPoint, 'id'>;
export type EvaluationStatus = 'COMPLETED' | 'PENDING' | 'RULE_NOT_CONFIGURED';
export interface CreateMeasurement {
  monitoringPointId: string;
  temperature: number;
  timestamp: string;
}
export interface Measurement extends Omit<CreateMeasurement, 'temperature'> {
  temperature: Decimal;
  id: string;
  evaluationStatus: EvaluationStatus;
}
export type AlertType = 'LOW_TEMPERATURE' | 'HIGH_TEMPERATURE';
export interface ThermalAlert {
  id: string;
  monitoringPointId: string;
  measurementId: string;
  temperature: Decimal;
  thresholdMin: Decimal;
  thresholdMax: Decimal;
  status: string;
  createdAt: string;
  type?: AlertType;
}
