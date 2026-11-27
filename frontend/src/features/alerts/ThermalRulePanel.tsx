import { useEffect, useState, type FormEvent } from 'react';
import { errorMessage, HttpError } from '../../api/http';
import { Notice } from '../../components/Feedback';
import { alertsApi } from '../../services/alerts';
import type { MonitoringPoint, ThermalRule } from '../../types/domain';

export function ThermalRulePanel({
  point,
  readOnly = false,
}: {
  point: MonitoringPoint;
  readOnly?: boolean;
}) {
  const [rule, setRule] = useState<ThermalRule | null>(null);
  const [min, setMin] = useState('');
  const [max, setMax] = useState('');
  const [feedback, setFeedback] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    const controller = new AbortController();
    setRule(null);
    setFeedback(null);
    setError(null);
    alertsApi
      .getRule(point.id, controller.signal)
      .then((found) => {
        setRule(found);
        setMin(String(found.thresholdMin));
        setMax(String(found.thresholdMax));
      })
      .catch((err) => {
        if (controller.signal.aborted) return;
        if (err instanceof HttpError && err.status === 404) {
          setMin('');
          setMax('');
          return;
        }
        setError(errorMessage(err));
      });
    return () => controller.abort();
  }, [point.id]);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    const minVal = Number(min);
    const maxVal = Number(max);
    if (!Number.isFinite(minVal) || !Number.isFinite(maxVal) || minVal >= maxVal) {
      setError('La temperatura mínima debe ser estrictamente menor que la máxima.');
      return;
    }
    setSaving(true);
    setError(null);
    setFeedback(null);
    try {
      const saved = await alertsApi.configureRule(point.id, { thresholdMin: minVal, thresholdMax: maxVal });
      setRule(saved);
      setFeedback(`Rango térmico guardado: ${String(saved.thresholdMin)} a ${String(saved.thresholdMax)} ${saved.unit}.`);
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setSaving(false);
    }
  }

  return (
    <section className="card form-card" style={{ marginBottom: '1.25rem' }}>
      <div className="card-heading">
        <div>
          <span className="eyebrow">HU-09 · RANGO TÉRMICO</span>
          <h2>Configurar límites para {point.code}</h2>
        </div>
      </div>
      {rule && (
        <p className="section-description">
          Rango vigente: <strong>{String(rule.thresholdMin)}</strong> a{' '}
          <strong>{String(rule.thresholdMax)}</strong> {rule.unit}
        </p>
      )}
      {feedback && <Notice>{feedback}</Notice>}
      {error && (
        <Notice tone="error" role="alert">
          {error}
        </Notice>
      )}
      {!readOnly && (
        <form className="form-grid" onSubmit={handleSubmit}>
          <div className="field">
            <label htmlFor="rule-min">Mínima (°C)</label>
            <input
              id="rule-min"
              type="number"
              step="any"
              value={min}
              onChange={(e) => setMin(e.target.value)}
              disabled={saving}
              required
            />
          </div>
          <div className="field">
            <label htmlFor="rule-max">Máxima (°C)</label>
            <input
              id="rule-max"
              type="number"
              step="any"
              value={max}
              onChange={(e) => setMax(e.target.value)}
              disabled={saving}
              required
            />
          </div>
          <button className="button button--primary" type="submit" disabled={saving}>
            {saving ? 'Guardando…' : 'Guardar rango'}
          </button>
        </form>
      )}
    </section>
  );
}
