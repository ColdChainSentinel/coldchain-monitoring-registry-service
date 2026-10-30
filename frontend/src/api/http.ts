import { parseJsonWithoutRounding, PrecisionError } from '../lib/decimal';

export interface Problem {
  detail?: string;
  title?: string;
  fields?: Record<string, string>;
  code?: string;
}

export class HttpError extends Error {
  constructor(
    public readonly status: number,
    public readonly problem?: Problem,
  ) {
    super(
      problem?.detail ||
        (
          {
            400: 'Revisa los campos enviados.',
            404: 'No se encontró el recurso o punto de monitoreo.',
            409: 'El registro entra en conflicto con datos existentes.',
            422: 'El punto no tiene una regla térmica configurada.',
            500: 'El servicio o el proxy devolvió un error. Comprueba su disponibilidad antes de repetir la operación.',
            502: 'El proxy no pudo obtener una respuesta del servicio.',
            503: 'El servicio o una dependencia no está disponible.',
            504: 'El proxy agotó el tiempo de espera del servicio.',
          } as Record<number, string>
        )[status] ||
        `La solicitud no pudo completarse (HTTP ${status}).`,
    );
    this.name = 'HttpError';
  }
}

export class ResponseError extends Error {
  constructor() {
    super('El servicio devolvió una respuesta incompatible. No se mostrarán datos incompletos.');
    this.name = 'ResponseError';
  }
}

export function errorMessage(error: unknown): string {
  if (error instanceof HttpError || error instanceof ResponseError || error instanceof PrecisionError)
    return error.message;
  if (error instanceof Error && ['TimeoutError', 'AbortError'].includes(error.name)) {
    return 'El servicio tardó demasiado en responder. Comprueba la conexión y actualiza los datos.';
  }
  return 'No se pudo comunicar con la API. Comprueba que el servicio esté disponible y la URL sea correcta.';
}

// No retries: POST measurements is NOT idempotent. Timeout allows both backend HTTP calls.
export function createHttpClient(baseUrl: string) {
  async function request<T>(
    method: 'GET' | 'POST',
    path: string,
    body?: unknown,
    signal?: AbortSignal,
  ): Promise<T> {
    const timeout = AbortSignal.timeout(20_000);
    const response = await fetch(`${baseUrl.replace(/\/+$/, '')}/${path.replace(/^\/+/, '')}`, {
      method,
      headers: {
        Accept: 'application/json',
        ...(method === 'POST' ? { 'Content-Type': 'application/json' } : {}),
      },
      ...(body === undefined ? {} : { body: JSON.stringify(body) }),
      signal: signal ? AbortSignal.any([signal, timeout]) : timeout,
    });
    if (!response.ok) {
      const problem: unknown = await response.json().catch(() => undefined);
      let details: Problem | undefined;
      if (problem && typeof problem === 'object') {
        const raw = problem as Record<string, unknown>;
        details = {
          detail: typeof raw.detail === 'string' ? raw.detail : undefined,
          fields:
            raw.fields && typeof raw.fields === 'object'
              ? Object.fromEntries(
                  Object.entries(raw.fields).filter(
                    (entry): entry is [string, string] => typeof entry[1] === 'string',
                  ),
                )
              : undefined,
        };
      }
      throw new HttpError(response.status, details);
    }
    try {
      return parseJsonWithoutRounding(await response.text()) as T;
    } catch (error) {
      if (error instanceof PrecisionError) throw error;
      throw new ResponseError();
    }
  }
  return {
    get: <T>(path: string, signal?: AbortSignal) => request<T>('GET', path, undefined, signal),
    post: <T>(path: string, body: unknown) => request<T>('POST', path, body),
  };
}
