import type { CreatePoint } from '../types/domain';
import { decimalKey } from './decimal';

export function validatePoint(input: { code: string; name: string; type: string }) {
  const fields: Record<string, string> = {};
  if (!/^[A-Za-z0-9_-]{1,40}$/.test(input.code.trim()))
    fields.code = 'Usa de 1 a 40 letras ASCII, números, guion o guion bajo.';
  if (!input.name.trim() || input.name.trim().length > 120)
    fields.name = 'Escribe un nombre de 1 a 120 caracteres.';
  if (!['ALMACENAMIENTO', 'TRANSPORTE'].includes(input.type)) fields.type = 'Selecciona un tipo de punto.';
  return fields;
}
export const pointPayload = (input: { code: string; name: string; type: string }): CreatePoint => ({
  code: input.code.trim(),
  name: input.name.trim(),
  type: input.type as CreatePoint['type'],
});

/**
 * Rango operativo admitido para lecturas y límites térmicos (°C). Cubre congelados
 * (hasta -40 °C) y la exposición máxima razonable de un vehículo o almacén (50 °C).
 * El backend aplica el mismo rango.
 */
export const TEMPERATURE_MIN = -40;
export const TEMPERATURE_MAX = 50;
export const TEMPERATURE_DECIMALS = 2;
export const TEMPERATURE_RANGE_TEXT = `Entre ${TEMPERATURE_MIN} °C y ${TEMPERATURE_MAX} °C, con hasta ${TEMPERATURE_DECIMALS} decimales.`;

export function parseTemperature(input: string): number {
  const normalized = input.trim().replace(',', '.');
  if (!/^[+-]?(?:\d{1,2}(?:\.\d*)?|\.\d+)$/.test(normalized)) {
    throw new Error(`Escribe una temperatura válida. ${TEMPERATURE_RANGE_TEXT}`);
  }
  const decimals = normalized.split('.')[1] ?? '';
  if (decimals.replace(/0+$/, '').length > TEMPERATURE_DECIMALS) {
    throw new Error(`Usa como máximo ${TEMPERATURE_DECIMALS} decimales.`);
  }
  const value = Number(normalized);
  if (!Number.isFinite(value) || decimalKey(normalized) !== decimalKey(String(value))) {
    throw new Error(`Escribe una temperatura válida. ${TEMPERATURE_RANGE_TEXT}`);
  }
  if (value < TEMPERATURE_MIN || value > TEMPERATURE_MAX) {
    throw new Error(`La temperatura debe estar entre ${TEMPERATURE_MIN} °C y ${TEMPERATURE_MAX} °C.`);
  }
  return value === 0 ? 0 : value;
}
export function parseLocalTimestamp(input: string): string {
  const value = new Date(input);
  if (
    !/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(:\d{2})?$/.test(input) ||
    !Number.isFinite(value.getTime()) ||
    value.getFullYear() < 1 ||
    value.getFullYear() > 9999
  ) {
    throw new Error('Selecciona una fecha y hora local válidas.');
  }
  const [day, time] = input.split('T');
  const expected = `${day}T${time.length === 5 ? `${time}:00` : time}`;
  const actual = new Date(value.getTime() - value.getTimezoneOffset() * 60_000).toISOString().slice(0, 19);
  if (actual !== expected) throw new Error('La fecha u hora no existe en la zona horaria de tu navegador.');
  return value.toISOString();
}
