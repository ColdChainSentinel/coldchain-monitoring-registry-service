import { parseJsonWithoutRounding, PrecisionError } from '../lib/decimal';

const TOKEN_KEY = 'coldchain.session.token';
let unauthorizedHandler: (() => void) | null = null;

export function getAuthToken(): string | null {
  try {
    return window.localStorage.getItem(TOKEN_KEY);
  } catch {
    return null;
  }
}

export function setAuthToken(token: string | null): void {
  try {
    if (token) {
      window.localStorage.setItem(TOKEN_KEY, token);
    } else {
      window.localStorage.removeItem(TOKEN_KEY);
    }
  } catch {
    // Ignore storage unavailability in restricted environments.
  }
}

export function setUnauthorizedHandler(handler: (() => void) | null): void {
  unauthorizedHandler = handler;
}

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
            401: 'Tu sesión expiró o las credenciales son inválidas.',
            403: 'No tienes permisos para realizar esta acción o consultar este punto.',
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
    method: 'GET' | 'POST' | 'PUT' | 'PATCH',
    path: string,
    body?: unknown,
    signal?: AbortSignal,
  ): Promise<T> {
    const timeout = AbortSignal.timeout(20_000);
    const token = getAuthToken();
    const response = await fetch(`${baseUrl.replace(/\/+$/, '')}/${path.replace(/^\/+/, '')}`, {
      method,
      headers: {
        Accept: 'application/json',
        ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      ...(body === undefined ? {} : { body: JSON.stringify(body) }),
      signal: signal ? AbortSignal.any([signal, timeout]) : timeout,
    });
    if (!response.ok) {
      if (response.status === 401 && !path.includes('/auth/login')) {
        setAuthToken(null);
        unauthorizedHandler?.();
      }
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
    if (response.status === 204) {
      return undefined as T;
    }
    try {
      const text = await response.text();
      if (!text) return undefined as T;
      return parseJsonWithoutRounding(text) as T;
    } catch (error) {
      if (error instanceof PrecisionError) throw error;
      throw new ResponseError();
    }
  }
  return {
    get: <T>(path: string, signal?: AbortSignal) => request<T>('GET', path, undefined, signal),
    post: <T>(path: string, body?: unknown) => request<T>('POST', path, body),
    put: <T>(path: string, body: unknown) => request<T>('PUT', path, body),
    patch: <T>(path: string, body: unknown) => request<T>('PATCH', path, body),
  };
}
