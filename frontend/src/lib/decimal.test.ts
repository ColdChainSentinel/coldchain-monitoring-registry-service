import { describe, expect, it, vi } from 'vitest';
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
  it('informa navegadores sin contexto numérico en lugar de perder precisión silenciosamente', () => {
    const native = JSON.parse;
    vi.spyOn(JSON, 'parse').mockImplementation((source, reviver) =>
      native(source, (key, value) => (reviver ? reviver(key, value) : value)),
    );
    expect(() => parseJsonWithoutRounding('{"temperature":3.75}')).toThrow(PrecisionError);
  });
});
