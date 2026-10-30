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

// JSON.parse source context is supported by current browsers and Node 22.
// Keep only values that cannot round-trip safely as exact source tokens.
export function parseJsonWithoutRounding(text: string): unknown {
  const parse = JSON.parse as (
    source: string,
    reviver: (key: string, value: unknown, context?: { source?: string }) => unknown,
  ) => unknown;
  return parse(text, (_key, value, context) => {
    if (typeof value !== 'number') return value;
    if (!context?.source) throw new PrecisionError();
    return !Number.isFinite(value) || decimalKey(context.source) !== decimalKey(String(value))
      ? new ExactDecimal(context.source)
      : value;
  });
}
