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

export type Role = 'ADMIN' | 'SUPERVISOR' | 'OPERADOR' | 'CONDUCTOR';

export interface AuthSession {
  userId: string;
  username: string;
  fullName: string;
  role: Role;
  allowedPointIds: string[];
  token?: string;
  message?: string;
}

export interface UserAccount {
  id: string;
  username: string;
  fullName: string;
  role: Role;
  active: boolean;
  allowedPointIds: string[];
  createdAt: string;
}

export interface CreateUser {
  username: string;
  password: string;
  fullName: string;
  role: Role;
  allowedPointIds: string[];
}

export interface UpdatePermissions {
  role: Role;
  allowedPointIds: string[];
}
