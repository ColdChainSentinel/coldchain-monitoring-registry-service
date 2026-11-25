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
    <div className="workspace" style={{ maxWidth: 460, margin: '4rem auto' }}>
      <section className="card">
        <div className="card-heading">
          <div>
            <span className="eyebrow">COLDCHAIN SENTINEL</span>
            <h1>Iniciar sesión</h1>
          </div>
          <Icon name="snow" />
        </div>
        <p className="section-description">
          Accede con tu cuenta registrada para consultar puntos de monitoreo, mediciones y alertas.
        </p>
        {error && (
          <Notice tone="error" role="alert">
            {error}
          </Notice>
        )}
        <form className="form-grid" onSubmit={handleSubmit}>
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
          <button className="button button--primary" type="submit" disabled={submitting}>
            {submitting ? 'Ingresando…' : 'Iniciar sesión'}
          </button>
        </form>
      </section>
    </div>
  );
}
