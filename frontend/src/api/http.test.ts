import { afterEach, describe, expect, it, vi } from 'vitest';
import { createHttpClient, HttpError } from './http';

afterEach(() => vi.unstubAllGlobals());

describe('cliente HTTP', () => {
  it('usa la URL configurable y un timeout, y devuelve JSON', async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response('{"status":"UP"}'));
    vi.stubGlobal('fetch', fetchMock);
    const result = await createHttpClient('http://localhost:8081/').get('/actuator/health');
    expect(result).toEqual({ status: 'UP' });
    expect(fetchMock).toHaveBeenCalledWith('http://localhost:8081/actuator/health', {
      method: 'GET',
      headers: { Accept: 'application/json' },
      signal: expect.any(AbortSignal),
    });
  });

  it('propaga errores HTTP sin simular una respuesta saludable', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('', { status: 503 })));
    await expect(createHttpClient('/alerts').get('/actuator/health')).rejects.toEqual(new HttpError(503));
  });

  it('propaga fallos de red', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Network error')));
    await expect(createHttpClient('/registry').get('/actuator/health')).rejects.toThrow('Network error');
  });

  it('envía JSON una sola vez al registrar y conserva Problem Details', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(
        JSON.stringify({
          detail: 'Datos inválidos',
          fields: { code: 'Código obligatorio' },
        }),
        { status: 400 },
      ),
    );
    vi.stubGlobal('fetch', fetchMock);
    await expect(createHttpClient('/registry').post('/api/v1/monitoring-points', {})).rejects.toMatchObject({
      status: 400,
      problem: { fields: { code: 'Código obligatorio' } },
    });
    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(fetchMock).toHaveBeenCalledWith(
      '/registry/api/v1/monitoring-points',
      expect.objectContaining({
        method: 'POST',
        body: '{}',
        headers: { Accept: 'application/json', 'Content-Type': 'application/json' },
      }),
    );
  });

  it('rechaza JSON inválido aunque el servidor responda 200', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('<html>proxy</html>')));
    await expect(createHttpClient('/alerts').get('/api/alerts')).rejects.toThrow('respuesta incompatible');
  });
});
