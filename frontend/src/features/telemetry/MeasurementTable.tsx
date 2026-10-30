import { EmptyState } from '../../components/Feedback';
import { formatDate, formatTemperature } from '../../lib/format';
import type { Measurement, ThermalAlert } from '../../types/domain';
import { AlertAssociation, EvaluationBadge } from './MeasurementStatus';

export function MeasurementTable({
  measurements,
  alerts,
  alertsReady,
}: {
  measurements: Measurement[];
  alerts: ThermalAlert[] | null;
  alertsReady: boolean;
}) {
  if (!measurements.length)
    return (
      <EmptyState title="Sin mediciones para este punto" icon="thermometer">
        Introduce una medición manual o espera las lecturas enviadas por un sensor.
      </EmptyState>
    );
  return (
    <div className="table-scroll" tabIndex={0} aria-label="Tabla de mediciones, desplazable horizontalmente">
      <table>
        <caption className="sr-only">Mediciones del punto seleccionado, más recientes primero</caption>
        <thead>
          <tr>
            <th scope="col">Fecha de medición · local</th>
            <th scope="col">Temperatura</th>
            <th scope="col">Evaluación</th>
            <th scope="col">Alerta asociada</th>
          </tr>
        </thead>
        <tbody>
          {[...measurements].reverse().map((row) => (
            <tr key={row.id}>
              <td>
                <time dateTime={row.timestamp} title={row.timestamp}>
                  {formatDate(row.timestamp)}
                </time>
                <code className="record-id" title={row.id}>
                  {row.id}
                </code>
              </td>
              <td className="temperature-value">{formatTemperature(row.temperature)}</td>
              <td>
                <EvaluationBadge status={row.evaluationStatus} />
              </td>
              <td>
                <AlertAssociation measurement={row} alerts={alerts} alertsReady={alertsReady} />
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <p className="table-hint">Desliza horizontalmente para ver todas las columnas.</p>
    </div>
  );
}
