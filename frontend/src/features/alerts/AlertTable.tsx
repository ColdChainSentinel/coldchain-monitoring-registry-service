import { EmptyState } from '../../components/Feedback';
import { formatDate, formatTemperature } from '../../lib/format';
import type { ThermalAlert } from '../../types/domain';

export function AlertTable({ alerts }: { alerts: ThermalAlert[] }) {
  if (!alerts.length)
    return (
      <EmptyState title="Sin alertas registradas para este punto" icon="bell">
        Esta consulta vacía no confirma que todas las mediciones hayan sido evaluadas.
      </EmptyState>
    );
  return (
    <div className="table-scroll" tabIndex={0} aria-label="Tabla de alertas, desplazable horizontalmente">
      <table>
        <caption className="sr-only">Alertas del punto seleccionado, más recientes primero</caption>
        <thead>
          <tr>
            <th scope="col">Registrada · hora local</th>
            <th scope="col">Temperatura</th>
            <th scope="col">Tipo</th>
            <th scope="col">Límites almacenados</th>
            <th scope="col">Estado</th>
          </tr>
        </thead>
        <tbody>
          {[...alerts].reverse().map((alert) => (
            <tr key={alert.id}>
              <td>
                <time dateTime={alert.createdAt} title={alert.createdAt}>
                  {formatDate(alert.createdAt)}
                </time>
                <code className="record-id" title={`Medición: ${alert.measurementId}`}>
                  {alert.measurementId}
                </code>
              </td>
              <td className="temperature-value">{formatTemperature(alert.temperature)}</td>
              <td>
                <span
                  className={`badge ${alert.type === 'LOW_TEMPERATURE' ? 'badge--cold' : alert.type === 'HIGH_TEMPERATURE' ? 'badge--danger' : 'badge--neutral'}`}
                  title={alert.type}
                >
                  {alert.type === 'LOW_TEMPERATURE'
                    ? 'Temperatura baja'
                    : alert.type === 'HIGH_TEMPERATURE'
                      ? 'Temperatura alta'
                      : 'Tipo no informado'}
                </span>
              </td>
              <td className="range-value">
                {formatTemperature(alert.thresholdMin)} <span aria-hidden="true">—</span>{' '}
                {formatTemperature(alert.thresholdMax)}
              </td>
              <td>
                <span className="badge badge--warning">
                  {alert.status === 'OPEN' ? 'Abierta (OPEN)' : alert.status}
                </span>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <p className="table-hint">Desliza horizontalmente para ver todas las columnas.</p>
    </div>
  );
}
