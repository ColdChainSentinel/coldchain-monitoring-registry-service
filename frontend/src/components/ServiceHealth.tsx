import { services, type Health } from '../api/services';
import { useResource } from '../hooks/useResource';

const names = { registry: 'Registry', telemetry: 'Telemetría', alerts: 'Alertas' };
const loadHealth = (signal: AbortSignal) =>
  Promise.all(
    Object.entries(services).map(async ([name, client]) => {
      try {
        const health = await client.get<Health>('/actuator/health', signal);
        return { name: name as keyof typeof names, available: health.status === 'UP' };
      } catch {
        return { name: name as keyof typeof names, available: false };
      }
    }),
  );

export function ServiceHealth({ revision }: { revision: number }) {
  const health = useResource('health', loadHealth, revision);
  return (
    <div className="service-health" aria-label="Última comprobación de salud de APIs">
      {(Object.keys(names) as Array<keyof typeof names>).map((name) => {
        const checked = health.data?.find((item) => item.name === name);
        const status = health.loading ? 'Comprobando' : checked?.available ? 'Disponible' : 'No disponible';
        return (
          <span
            key={name}
            className={`health-pill ${!health.loading && checked?.available ? 'health-pill--up' : !health.loading ? 'health-pill--down' : ''}`}
          >
            <span className="status-dot" />
            <span>{names[name]}</span>
            <span className="sr-only">: {status}</span>
            <span className="health-label" aria-hidden="true">
              {status}
            </span>
          </span>
        );
      })}
    </div>
  );
}
