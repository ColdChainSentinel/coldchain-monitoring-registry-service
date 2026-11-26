import { describe, expect, it } from 'vitest';
import { ExactDecimal, parseJsonWithoutRounding, PrecisionError } from './decimal';
import { formatTemperature } from './format';

describe('lectura exacta de números JSON', () => {
  it('conserva una temperatura apenas superior al límite sin mostrarla como igual', () => {
    const result = parseJsonWithoutRounding('{"temperature":8.000000000000000001}') as {
      temperature: ExactDecimal;
    };
    expect(result.temperature).toBeInstanceOf(ExactDecimal);
    expect(formatTemperature(result.temperature)).toBe('8.000000000000000001 °C');
  });
  it('conserva exponentes fuera del rango de Number sin inventar Infinity o cero', () => {
    const result = parseJsonWithoutRounding('[1e131071,1e-16383]') as ExactDecimal[];
    expect(result.map(formatTemperature)).toEqual(['1e131071 °C', '1e-16383 °C']);
  });
  it('mantiene números habituales y no redondea temperaturas pequeñas en pantalla', () => {
    expect(parseJsonWithoutRounding('[3.75,0,-20,2.000]')).toEqual([3.75, 0, -20, 2]);
    expect(formatTemperature(1e-21)).toBe('1e-21 °C');
  });
  it('sin source text (Firefox < 135) lee números seguros y rechaza los que perderían precisión', () => {
    expect(parseJsonWithoutRounding('{"temperature":3.75,"code":"123456789012345678901"}', false)).toEqual({
      temperature: 3.75,
      code: '123456789012345678901',
    });
    expect(() => parseJsonWithoutRounding('{"temperature":8.000000000000000001}', false)).toThrow(
      PrecisionError,
    );
    expect(() => parseJsonWithoutRounding('[1e131071]', false)).toThrow(PrecisionError);
  });
});
