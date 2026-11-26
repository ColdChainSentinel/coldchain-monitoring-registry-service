import { useEffect, useState, type FormEvent } from 'react';
import { errorMessage, HttpError } from '../../api/http';
import { Notice } from '../../components/Feedback';
import { alertsApi } from '../../services/alerts';
import { parseTemperature, TEMPERATURE_MAX, TEMPERATURE_MIN, TEMPERATURE_RANGE_TEXT } from '../../lib/validation';
import type { MonitoringPoint, ThermalRule } from '../../types/domain';

export function ThermalRulePanel({ point }: { point: MonitoringPoint }) {
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
        if (err instanceof HttpError && err.status === 404) {
          setMin('');
          setMax('');
        }
      });
    return () => controller.abort();
  }, [point.id]);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    let minVal: number;
    let maxVal: number;
    try {
      minVal = parseTemperature(min);
      maxVal = parseTemperature(max);
    } catch (failure) {
      setError((failure as Error).message);
      return;
    }
    if (minVal >= maxVal) {
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
    <section className="card" style={{ marginBottom: '1.25rem' }}>
      <div className="card-heading">
        <div>
          <span className="eyebrow">RANGO TÉRMICO</span>
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
      <form className="form-grid" onSubmit={handleSubmit}>
        <div className="field">
          <label htmlFor="rule-min">Mínima (°C)</label>
          <input
            id="rule-min"
            type="number"
            step="0.01"
            min={TEMPERATURE_MIN}
            max={TEMPERATURE_MAX}
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
            step="0.01"
            min={TEMPERATURE_MIN}
            max={TEMPERATURE_MAX}
            value={max}
            onChange={(e) => setMax(e.target.value)}
            disabled={saving}
            required
          />
        </div>
        <p className="field-help">{TEMPERATURE_RANGE_TEXT}</p>
        <button className="button button--primary" type="submit" disabled={saving}>
          {saving ? 'Guardando…' : 'Guardar rango'}
        </button>
      </form>
    </section>
  );
}
