export class HttpError extends Error {
  constructor(public readonly status: number) {
    super(`HTTP ${status}`);
    this.name = 'HttpError';
  }
}

// fetch nativo: sin dependencia adicional ni credenciales embebidas.
export function createHttpClient(baseUrl: string) {
  return {
    async get<T>(path: string): Promise<T> {
      const response = await fetch(
        `${baseUrl.replace(/\/+$/, '')}/${path.replace(/^\/+/, '')}`,
        { headers: { Accept: 'application/json' }, signal: AbortSignal.timeout(5000) },
      );
      if (!response.ok) throw new HttpError(response.status);
      return response.json() as Promise<T>;
    },
  };
}
