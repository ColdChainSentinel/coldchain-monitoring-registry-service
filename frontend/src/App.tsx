import { useEffect, useState } from 'react';
import { getAuthToken, setAuthToken, setUnauthorizedHandler } from './api/http';
import { LoginPage } from './features/auth/LoginPage';
import { HomePage } from './pages/HomePage';
import { identityApi } from './services/identity';
import type { AuthSession } from './types/domain';

export function App() {
  const [session, setSession] = useState<AuthSession | null>(null);
  const [checking, setChecking] = useState(() => Boolean(getAuthToken()));

  useEffect(() => {
    setUnauthorizedHandler(() => setSession(null));
    return () => setUnauthorizedHandler(null);
  }, []);

  useEffect(() => {
    if (!getAuthToken()) {
      setChecking(false);
      return;
    }
    const controller = new AbortController();
    identityApi
      .me(controller.signal)
      .then((me) => setSession(me))
      .catch(() => {
        setAuthToken(null);
        setSession(null);
      })
      .finally(() => setChecking(false));
    return () => controller.abort();
  }, []);

  function handleLoginSuccess(loggedIn: AuthSession) {
    if (loggedIn.token) {
      setAuthToken(loggedIn.token);
    }
    setSession(loggedIn);
  }

  async function handleLogout() {
    try {
      await identityApi.logout();
    } catch {
      // Clear local token even if session already expired.
    } finally {
      setAuthToken(null);
      setSession(null);
    }
  }

  if (checking) {
    return (
      <div className="workspace" style={{ maxWidth: 460, margin: '4rem auto' }}>
        <section className="card form-card">
          <p>Verificando sesión…</p>
        </section>
      </div>
    );
  }

  if (!session) {
    return <LoginPage onSuccess={handleLoginSuccess} />;
  }

  return <HomePage session={session} onLogout={handleLogout} />;
}
