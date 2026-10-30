import { useState, type FormEvent } from 'react';
import { HttpError, errorMessage } from '../../api/http';
import { FieldError, Notice } from '../../components/Feedback';
import { Icon } from '../../components/Icon';
import { localNow } from '../../lib/format';
import { parseLocalTimestamp, parseTemperature } from '../../lib/validation';
import type { CreateMeasurement, Measurement, MonitoringPoint } from '../../types/domain';

export function MeasurementForm({
  point,
  onCreate,
}: {
  point: MonitoringPoint;
  onCreate: (input: CreateMeasurement) => Promise<Measurement>;
}) {
  const [temperature, setTemperature] = useState('');
  const [timestamp, setTimestamp] = useState(localNow);
  const [fields, setFields] = useState<Record<string, string>>({});
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  async function submit(event: FormEvent) {
    event.preventDefault();
    if (busy) return;
    const invalid: Record<string, string> = {};
    let value = 0,
      instant = '';
    try {
      value = parseTemperature(temperature);
    } catch (failure) {
      invalid.temperature = (failure as Error).message;
    }
    try {
      instant = parseLocalTimestamp(timestamp);
    } catch (failure) {
      invalid.timestamp = (failure as Error).message;
    }
    setFields(invalid);
    setError('');
    if (Object.keys(invalid).length) return;
    setBusy(true);
    try {
      await onCreate({ monitoringPointId: point.id, temperature: value, timestamp: instant });
      setTemperature('');
      setTimestamp(localNow());
    } catch (failure) {
      if (failure instanceof HttpError) setFields(failure.problem?.fields || {});
      setError(errorMessage(failure));
    } finally {
      setBusy(false);
    }
  }
  return (
    <section className="card form-card" aria-labelledby="measurement-form-heading">
      <div className="card-heading">
        <div>
          <span className="eyebrow">ENTRADA MANUAL · SIMULACIÓN IOT</span>
          <h2 id="measurement-form-heading">Registrar medición</h2>
        </div>
        <Icon name="thermometer" />
      </div>
      <p className="section-description">
        Destino: <strong>{point.code}</strong> · {point.name}. La evaluación se solicita automáticamente al
        guardar.
      </p>
      {error && (
        <Notice tone="error" role="alert">
          <strong>No se pudo confirmar la medición.</strong>
          <p>{error}</p>
          <p>
            Puede haberse guardado si se perdió la respuesta. Actualiza las mediciones antes de enviarla otra
            vez.
          </p>
        </Notice>
      )}
      <form onSubmit={submit} noValidate aria-busy={busy}>
        <div className="field">
          <label htmlFor="temperature">Temperatura (°C)</label>
          <input
            id="temperature"
            name="temperature"
            type="text"
            inputMode="decimal"
            autoComplete="off"
            value={temperature}
            required
            onChange={(e) => setTemperature(e.target.value)}
            aria-invalid={!!fields.temperature}
            disabled={busy}
            aria-describedby={`temperature-help${fields.temperature ? ' temperature-error' : ''}`}
          />
          <span className="field-help" id="temperature-help">
            Se admiten valores negativos y cero. No hay límites universales predefinidos.
          </span>
          <FieldError id="temperature-error" message={fields.temperature} />
        </div>
        <div className="field">
          <label htmlFor="measurement-timestamp">Fecha y hora de medición</label>
          <input
            id="measurement-timestamp"
            name="timestamp"
            type="datetime-local"
            step="1"
            value={timestamp}
            required
            onChange={(e) => setTimestamp(e.target.value)}
            aria-invalid={!!fields.timestamp}
            disabled={busy}
            aria-describedby={`timestamp-help${fields.timestamp ? ' timestamp-error' : ''}`}
          />
          <span id="timestamp-help" className="field-help">
            Hora local del navegador ({Intl.DateTimeFormat().resolvedOptions().timeZone}). Se envía en UTC.
          </span>
          <FieldError id="timestamp-error" message={fields.timestamp} />
        </div>
        <button className="button button--primary" disabled={busy} type="submit">
          <Icon name="plus" />
          {busy ? 'Guardando medición…' : 'Guardar medición'}
        </button>
      </form>
    </section>
  );
}
