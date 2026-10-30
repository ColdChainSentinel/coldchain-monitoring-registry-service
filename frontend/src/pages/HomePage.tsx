import { useState } from 'react';
import { services, type Health } from '../api/services';

export function HomePage() {
  const [results, setResults] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(false);

  async function checkHealth() {
    setLoading(true);
    const entries = await Promise.all(Object.entries(services).map(async ([name, client]) => {
      try {
        const health = await client.get<Health>('/actuator/health');
        return [name, health.status];
      } catch (error) {
        return [name, error instanceof Error ? error.message : 'No disponible'];
      }
    }));
    setResults(Object.fromEntries(entries));
    setLoading(false);
  }

  return (
    <main>
      <h1>ColdChain Sentinel</h1>
      <p>Base de integración del MVP. El dashboard se desarrollará en otra fase.</p>
      <button onClick={checkHealth} disabled={loading}>
        {loading ? 'Comprobando…' : 'Comprobar servicios'}
      </button>
      <ul aria-live="polite">
        {Object.keys(services).map((name) => (
          <li key={name}><strong>{name}</strong>: {results[name] ?? 'Sin comprobar'}</li>
        ))}
      </ul>
    </main>
  );
}
