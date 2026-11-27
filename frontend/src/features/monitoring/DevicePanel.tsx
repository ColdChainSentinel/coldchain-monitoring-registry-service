import { useCallback, useState, type FormEvent } from 'react';
import { errorMessage } from '../../api/http';
import { Notice, ResourceFeedback } from '../../components/Feedback';
import { useResource } from '../../hooks/useResource';
import { monitoringApi } from '../../services/monitoring';
import type { MonitoringPoint } from '../../types/domain';

export function DevicePanel({
  point,
  readOnly = false,
}: {
  point: MonitoringPoint;
  readOnly?: boolean;
}) {
  const [revision, setRevision] = useState(0);
  const loadDevices = useCallback(
    (signal: AbortSignal) => monitoringApi.listDevices(point.id, signal),
    [point.id],
  );
  const devices = useResource(`devices:${point.id}`, loadDevices, revision);

  const [code, setCode] = useState('');
  const [feedback, setFeedback] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setSaving(true);
    setError(null);
    setFeedback(null);
    try {
      const created = await monitoringApi.createDevice({ code: code.trim(), monitoringPointId: point.id });
      setCode('');
      setFeedback(`Dispositivo ${created.code} registrado (${created.status}) en ${created.monitoringPointCode}.`);
      setRevision((v) => v + 1);
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setSaving(false);
    }
  }

  return (
    <section className="card form-card" style={{ marginTop: '1.25rem' }}>
      <div className="card-heading">
        <div>
          <span className="eyebrow">HU-06 · DISPOSITIVOS</span>
          <h2>Dispositivos asociados a {point.code}</h2>
        </div>
      </div>
      {feedback && <Notice>{feedback}</Notice>}
      {error && (
        <Notice tone="error" role="alert">
          {error}
        </Notice>
      )}
      <ResourceFeedback resource={devices} onRetry={devices.reload} />
      {devices.data && (
        <p className="section-description">
          {devices.data.length === 0
            ? 'Sin dispositivos registrados para este punto.'
            : devices.data.map((d) => `${d.code} (${d.status})`).join(' · ')}
        </p>
      )}
      {!readOnly && (
        <form className="form-grid" onSubmit={handleSubmit} style={{ marginTop: '0.75rem' }}>
          <div className="field">
            <label htmlFor="device-code">Identificador de dispositivo</label>
            <input
              id="device-code"
              value={code}
              onChange={(e) => setCode(e.target.value)}
              placeholder="ej. SEN-01"
              disabled={saving}
              required
            />
          </div>
          <button className="button button--primary" type="submit" disabled={saving}>
            {saving ? 'Registrando…' : 'Registrar dispositivo'}
          </button>
        </form>
      )}
    </section>
  );
}
