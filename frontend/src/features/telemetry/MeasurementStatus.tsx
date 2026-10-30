import type { EvaluationStatus, Measurement, ThermalAlert } from '../../types/domain';
import { Notice } from '../../components/Feedback';

const evaluations: Record<EvaluationStatus, { label: string; tone: string }> = {
  COMPLETED: { label: 'Evaluada', tone: 'success' },
  PENDING: { label: 'Evaluación pendiente', tone: 'warning' },
  RULE_NOT_CONFIGURED: { label: 'Sin regla térmica', tone: 'warning' },
};
export function EvaluationBadge({ status }: { status: EvaluationStatus }) {
  const badge = evaluations[status];
  return (
    <span className={`badge badge--${badge.tone}`} title={status}>
      {badge.label}
    </span>
  );
}
export function AlertAssociation({
  measurement,
  alerts,
  alertsReady,
}: {
  measurement: Measurement;
  alerts: ThermalAlert[] | null;
  alertsReady: boolean;
}) {
  if (!alertsReady) return <span className="muted">Consulta de alertas no confirmada</span>;
  const associated = alerts?.find((alert) => alert.measurementId === measurement.id);
  if (associated) return <span className="badge badge--danger">Alerta registrada</span>;
  if (measurement.evaluationStatus === 'COMPLETED')
    return <span className="muted">Sin alerta registrada</span>;
  return <span className="muted">Resultado no confirmado</span>;
}
export function MeasurementReceipt({ measurement }: { measurement: Measurement }) {
  const status = measurement.evaluationStatus;
  return (
    <Notice tone={status === 'COMPLETED' ? 'success' : 'warning'}>
      <strong>
        {status === 'COMPLETED'
          ? 'Medición registrada y evaluada.'
          : status === 'PENDING'
            ? 'Medición registrada; evaluación pendiente.'
            : 'Medición registrada; falta una regla térmica.'}
      </strong>
      <p>
        {status === 'COMPLETED'
          ? 'COMPLETED confirma la evaluación, no la ausencia de una alerta. Consulta las alertas asociadas.'
          : status === 'PENDING'
            ? 'PENDING no significa NO_ALERT. La medición ya está guardada: no la vuelvas a enviar para reintentar su evaluación.'
            : 'Un responsable debe configurar los límites de este punto fuera del frontend. No se aplicaron límites por defecto.'}
      </p>
      <span className="receipt-id">ID: {measurement.id}</span>
    </Notice>
  );
}
