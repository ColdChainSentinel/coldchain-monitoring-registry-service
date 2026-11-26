// Compare decimal values without expanding exponents or rounding through binary floats.
export function decimalKey(value: string): string {
  const [mantissa, exponent = '0'] = value.toLowerCase().split('e');
  const negative = mantissa.startsWith('-');
  const [integer, fraction = ''] = mantissa.replace(/^[+-]/, '').split('.');
  let digits = (integer + fraction).replace(/^0+/, '');
  if (!digits) return '0';
  let scale = fraction.length - Number(exponent);
  while (digits.endsWith('0')) {
    digits = digits.slice(0, -1);
    scale--;
  }
  return `${negative ? '-' : ''}${digits}:${scale}`;
}

export class ExactDecimal {
  constructor(public readonly raw: string) {}
}
export type Decimal = number | ExactDecimal;
export class PrecisionError extends Error {
  constructor() {
    super('Tu navegador no permite leer estos datos sin perder precisión. Actualízalo para continuar.');
    this.name = 'PrecisionError';
  }
}

type Reviver = (key: string, value: unknown, context?: { source?: string }) => unknown;
const parse = JSON.parse as (source: string, reviver?: Reviver) => unknown;

// JSON.parse source text access (ES2025) existe en Chromium 114+, Safari 18+ y
// Firefox 135+. Se detecta una sola vez.
export const supportsSourceText = (() => {
  try {
    let seen = false;
    parse('1', (_k, v, c) => ((seen = c?.source === '1'), v));
    return seen;
  } catch {
    return false;
  }
})();

const STRINGS = /"(?:[^"\\]|\\.)*"/g;
const NUMBERS = /-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?/g;

// Respaldo para navegadores sin source text (p. ej. Firefox < 135): revisa los
// literales numéricos del texto original y solo falla si alguno perdería
// precisión al convertirse en número de JavaScript.
function parseWithTokenCheck(text: string): unknown {
  const tokens = text.replace(STRINGS, '""').match(NUMBERS) || [];
  for (const token of tokens) {
    const value = Number(token);
    if (!Number.isFinite(value) || decimalKey(token) !== decimalKey(String(value))) throw new PrecisionError();
  }
  return JSON.parse(text);
}

// Keep only values that cannot round-trip safely as exact source tokens.
export function parseJsonWithoutRounding(text: string, withSource = supportsSourceText): unknown {
  if (!withSource) return parseWithTokenCheck(text);
  return parse(text, (_key, value, context) => {
    if (typeof value !== 'number') return value;
    if (!context?.source) throw new PrecisionError();
    return !Number.isFinite(value) || decimalKey(context.source) !== decimalKey(String(value))
      ? new ExactDecimal(context.source)
      : value;
  });
}
