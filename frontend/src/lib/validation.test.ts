import { describe, expect, it } from 'vitest';
import { parseLocalTimestamp, parseTemperature, pointPayload, validatePoint } from './validation';

describe('validaciones del contrato', () => {
  it('valida código, nombre y tipo del punto', () => {
    expect(Object.keys(validatePoint({ code: '', name: ' ', type: '' }))).toEqual(['code', 'name', 'type']);
    expect(validatePoint({ code: 'A'.repeat(41), name: 'N'.repeat(121), type: 'OTRO' })).toHaveProperty(
      'code',
    );
    expect(validatePoint({ code: 'CAM 01', name: 'Cámara', type: 'ALMACENAMIENTO' })).toHaveProperty('code');
    expect(validatePoint({ code: 'CAM_01-X', name: 'Cámara', type: 'ALMACENAMIENTO' })).toEqual({});
    expect(pointPayload({ code: ' cam-01 ', name: ' Cámara ', type: 'TRANSPORTE' })).toEqual({
      code: 'cam-01',
      name: 'Cámara',
      type: 'TRANSPORTE',
    });
  });

  it.each(['0', '-30', '3.75', '3,75', '.5', '+2', '1e2', '-0', '1.000', '0.0001'])(
    'acepta número sin inventar límites: %s',
    (value) => {
      expect(Number.isFinite(parseTemperature(value))).toBe(true);
    },
  );
  it.each([
    '',
    ' ',
    'NaN',
    'Infinity',
    'abc',
    '1,2,3',
    '1e999',
    '1e-999',
    '8.000000000000000001',
    '9007199254740993',
  ])('rechaza valor inválido o redondeo silencioso: %s', (value) => {
    expect(() => parseTemperature(value)).toThrow();
  });
  it('convierte fecha local a un instante UTC explícito', () => {
    const date = parseLocalTimestamp('2026-10-29T10:30:00');
    expect(date).toEqual(new Date('2026-10-29T10:30:00').toISOString());
    expect(date).toMatch(/Z$/);
  });
  it.each(['', 'invalid', '2026-02-30T10:00', '0000-01-01T00:00'])('rechaza fecha inválida: %s', (value) => {
    expect(() => parseLocalTimestamp(value)).toThrow();
  });
});
