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

export function parseTemperature(input: string): number {
  const normalized = input.trim().replace(',', '.');
  if (!/^[+-]?(?:\d+(?:\.\d*)?|\.\d+)(?:[eE][+-]?\d+)?$/.test(normalized)) {
    throw new Error('Escribe una temperatura numérica; se admiten negativos y cero.');
  }
  const value = Number(normalized);
  if (!Number.isFinite(value) || decimalKey(normalized) !== decimalKey(String(value))) {
    throw new Error(
      'El navegador no puede enviar este valor sin redondearlo. Usa un emisor que preserve su precisión.',
    );
  }
  return value;
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
