import { describe, expect, it, vi } from 'vitest';
import { alert, json, measurement, point } from '../test/fixtures';
import { alertsApi } from './alerts';
import { monitoringApi } from './monitoring';
import { telemetryApi } from './telemetry';

describe('clientes separados de APIs reales', () => {
  it('usa solo las rutas definidas y valida las respuestas', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(json([point]))
      .mockResolvedValueOnce(json(point, 201))
      .mockResolvedValueOnce(json([measurement]))
      .mockResolvedValueOnce(json(measurement, 201))
      .mockResolvedValueOnce(json([alert]));
    vi.stubGlobal('fetch', fetchMock);
    expect(await monitoringApi.list()).toEqual([point]);
    expect(await monitoringApi.create({ code: point.code, name: point.name, type: point.type })).toEqual(
      point,
    );
    expect(await telemetryApi.list(point.id)).toEqual([measurement]);
    expect(
      await telemetryApi.create({
        monitoringPointId: point.id,
        temperature: 9,
        timestamp: measurement.timestamp,
      }),
    ).toEqual(measurement);
    expect(await alertsApi.list(point.id)).toEqual([alert]);
    expect(fetchMock.mock.calls.map((call) => call[0])).toEqual([
      '/registry/api/v1/monitoring-points',
      '/registry/api/v1/monitoring-points',
      `/telemetry/api/measurements?monitoringPointId=${point.id}`,
      '/telemetry/api/measurements',
      `/alerts/api/alerts?monitoringPointId=${point.id}`,
    ]);
  });
  it('acepta listas vacías reales, no respuestas sin modelo', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValueOnce(json([])).mockResolvedValueOnce(json({})));
    expect(await alertsApi.list(point.id)).toEqual([]);
    await expect(monitoringApi.list()).rejects.toThrow('respuesta incompatible');
  });
  it('no sustituye errores 503 por arrays vacíos', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(json({ detail: 'DB no disponible' }, 503)));
    await expect(telemetryApi.list(point.id)).rejects.toMatchObject({ status: 503 });
  });
  it('admite type opcional sin inventar la clasificación', async () => {
    const { type: _type, ...legacyAlert } = alert;
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(json([legacyAlert])));
    expect((await alertsApi.list(point.id))[0].type).toBeUndefined();
  });

  it('conserva decimales reales de PostgreSQL y rechaza números como strings de API', async () => {
    const raw = JSON.stringify([alert]).replace('"temperature":9', '"temperature":8.000000000000000001');
    vi.stubGlobal(
      'fetch',
      vi
        .fn()
        .mockResolvedValueOnce(new Response(raw))
        .mockResolvedValueOnce(json([{ ...alert, temperature: '9' }])),
    );
    expect((await alertsApi.list(point.id))[0].temperature).toMatchObject({ raw: '8.000000000000000001' });
    await expect(alertsApi.list(point.id)).rejects.toThrow('respuesta incompatible');
  });
});
