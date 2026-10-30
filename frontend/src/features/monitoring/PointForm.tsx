import { useState, type FormEvent } from 'react';
import { HttpError, errorMessage } from '../../api/http';
import { FieldError, Notice } from '../../components/Feedback';
import { Icon } from '../../components/Icon';
import { pointPayload, validatePoint } from '../../lib/validation';
import type { CreatePoint, MonitoringPoint } from '../../types/domain';

export function PointForm({ onCreate }: { onCreate: (input: CreatePoint) => Promise<MonitoringPoint> }) {
  const [values, setValues] = useState({ code: '', name: '', type: '' });
  const [fields, setFields] = useState<Record<string, string>>({});
  const [error, setError] = useState('');
  const [created, setCreated] = useState<MonitoringPoint | null>(null);
  const [busy, setBusy] = useState(false);
  async function submit(event: FormEvent) {
    event.preventDefault();
    if (busy) return;
    const invalid = validatePoint(values);
    setFields(invalid);
    setError('');
    setCreated(null);
    if (Object.keys(invalid).length) return;
    setBusy(true);
    try {
      const point = await onCreate(pointPayload(values));
      setCreated(point);
      setValues({ code: '', name: '', type: '' });
    } catch (failure) {
      if (failure instanceof HttpError) setFields(failure.problem?.fields || {});
      setError(
        failure instanceof HttpError && failure.status === 409
          ? 'El código ya está registrado. Usa un código distinto.'
          : errorMessage(failure),
      );
    } finally {
      setBusy(false);
    }
  }
  return (
    <section className="card form-card" aria-labelledby="point-form-heading">
      <div className="card-heading">
        <div>
          <span className="eyebrow">MONITORING REGISTRY</span>
          <h2 id="point-form-heading">Registrar punto</h2>
        </div>
        <Icon name="plus" />
      </div>
      <p className="section-description">
        Identifica una cámara, almacén o transporte. Los límites térmicos se configuran por separado.
      </p>
      {created && (
        <Notice tone="success">
          <strong>Punto {created.code} registrado.</strong>
          <p>Ya puedes seleccionarlo para consultar o registrar mediciones.</p>
        </Notice>
      )}
      {error && (
        <Notice tone="error" role="alert">
          <strong>No se pudo confirmar el registro.</strong>
          <p>{error}</p>
          <p>Si falló la comunicación, actualiza la lista antes de volver a enviar.</p>
        </Notice>
      )}
      <form onSubmit={submit} noValidate aria-busy={busy}>
        <div className="field">
          <label htmlFor="point-code">Código del punto</label>
          <input
            id="point-code"
            name="code"
            value={values.code}
            maxLength={40}
            autoComplete="off"
            required
            onChange={(e) => setValues({ ...values, code: e.target.value })}
            aria-invalid={!!fields.code}
            aria-describedby={`code-help${fields.code ? ' code-error' : ''}`}
            disabled={busy}
          />
          <span id="code-help" className="field-help">
            Único. Letras, números, guion o guion bajo; máximo 40 caracteres.
          </span>
          <FieldError id="code-error" message={fields.code} />
        </div>
        <div className="field">
          <label htmlFor="point-name">Nombre</label>
          <input
            id="point-name"
            name="name"
            value={values.name}
            maxLength={120}
            required
            onChange={(e) => setValues({ ...values, name: e.target.value })}
            aria-invalid={!!fields.name}
            aria-describedby={fields.name ? 'name-error' : undefined}
            disabled={busy}
          />
          <FieldError id="name-error" message={fields.name} />
        </div>
        <div className="field">
          <label htmlFor="point-type">Tipo</label>
          <select
            id="point-type"
            name="type"
            value={values.type}
            required
            disabled={busy}
            onChange={(e) => setValues({ ...values, type: e.target.value })}
            aria-invalid={!!fields.type}
            aria-describedby={fields.type ? 'type-error' : undefined}
          >
            <option value="">Selecciona un tipo</option>
            <option value="ALMACENAMIENTO">Almacenamiento</option>
            <option value="TRANSPORTE">Transporte</option>
          </select>
          <FieldError id="type-error" message={fields.type} />
        </div>
        <button className="button button--primary" type="submit" disabled={busy}>
          <Icon name="plus" />
          {busy ? 'Registrando punto…' : 'Registrar punto'}
        </button>
      </form>
    </section>
  );
}
