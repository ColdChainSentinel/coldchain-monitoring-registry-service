import { useState, type FormEvent } from 'react';
import { errorMessage } from '../../api/http';
import { Notice } from '../../components/Feedback';
import { Icon } from '../../components/Icon';
import { identityApi } from '../../services/identity';
import type { AuthSession } from '../../types/domain';

export function LoginPage({ onSuccess }: { onSuccess: (session: AuthSession) => void }) {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    if (!username.trim() || !password) {
      setError('Ingresa tu identificador y contraseña.');
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      const session = await identityApi.login(username.trim(), password);
      onSuccess(session);
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="login-shell">
      <main className="login-panel">
        <section className="login-card">
          <div className="login-brand">
            <span className="brand-icon">
              <Icon name="snow" />
            </span>
            <div>
              <span className="eyebrow">COLDCHAIN SENTINEL</span>
              <h1>Iniciar sesión</h1>
            </div>
          </div>
          <p className="section-description">
            Accede con tu cuenta registrada para consultar puntos de monitoreo, mediciones y alertas.
          </p>
          {error && (
            <Notice tone="error" role="alert">
              {error}
            </Notice>
          )}
          <form className="form-grid login-form" onSubmit={handleSubmit}>
            <div className="field">
              <label htmlFor="login-username">Identificador</label>
              <input
                id="login-username"
                autoComplete="username"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                disabled={submitting}
                placeholder="ej. admin"
              />
            </div>
            <div className="field">
              <label htmlFor="login-password">Contraseña</label>
              <input
                id="login-password"
                type="password"
                autoComplete="current-password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                disabled={submitting}
              />
            </div>
            <button className="button button--primary login-submit" type="submit" disabled={submitting}>
              {submitting ? 'Ingresando…' : 'Iniciar sesión'}
            </button>
          </form>
        </section>
      </main>
      <aside className="login-visual" aria-hidden="true">
        <svg
          className="login-snowflake"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="0.95"
          strokeLinecap="round"
          strokeLinejoin="round"
        >
          <path d="M12 2v20M3.34 7l17.32 10M3.34 17L20.66 7M8 4l4 4 4-4M8 20l4-4 4 4M3 11l5-1-1-5M21 13l-5 1 1 5M3 13l5 1-1 5M21 11l-5-1 1-5" />
        </svg>
        <div className="login-visual-copy">
          <span className="login-visual-pill">CADENA DE FRÍO · MONITOREO CONTINUO</span>
          <h2>Visibilidad térmica en cada punto.</h2>
          <p>
            Supervisa cámaras de almacenamiento y unidades de transporte con trazabilidad de mediciones y
            alertas oportunas.
          </p>
        </div>
      </aside>
    </div>
  );
}
