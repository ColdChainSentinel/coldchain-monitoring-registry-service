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

  it.each([
    ['0', 0],
    ['-30', -30],
    ['3.75', 3.75],
    ['3,75', 3.75],
    ['.5', 0.5],
    ['+2', 2],
    ['-0', 0],
    ['1.000', 1],
    ['-40', -40],
    ['50', 50],
    ['49.99', 49.99],
  ])('acepta temperatura dentro del rango operativo: %s', (value, expected) => {
    expect(parseTemperature(value)).toBe(expected);
  });
  it.each([
    '',
    ' ',
    'NaN',
    'Infinity',
    'abc',
    '1,2,3',
    '1e2',
    '-40.01',
    '50.01',
    '120',
    '-273',
    '9007199254740993',
    '3.141',
    '0.0001',
  ])('rechaza valores fuera de rango, con más de 2 decimales o no numéricos: %s', (value) => {
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
