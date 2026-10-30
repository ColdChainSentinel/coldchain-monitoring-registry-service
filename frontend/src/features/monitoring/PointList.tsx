import { EmptyState } from '../../components/Feedback';
import { pointTypeLabel } from '../../lib/format';
import type { MonitoringPoint } from '../../types/domain';

export function PointList({
  points,
  selectedId,
  onSelect,
}: {
  points: MonitoringPoint[];
  selectedId: string | null;
  onSelect: (id: string) => void;
}) {
  if (!points.length)
    return (
      <EmptyState title="Todavía no hay puntos registrados" icon="point">
        Registra el primer punto para comenzar a introducir mediciones.
      </EmptyState>
    );
  return (
    <div className="point-list">
      {points.map((point) => (
        <article
          key={point.id}
          className={`point-item ${point.id === selectedId ? 'point-item--selected' : ''}`}
        >
          <div className="point-item-top">
            <span className="point-code">{point.code}</span>
            <span className="tag">{pointTypeLabel(point.type)}</span>
          </div>
          <h3>{point.name}</h3>
          <code className="record-id">{point.id}</code>
          <button
            className="text-button"
            onClick={() => onSelect(point.id)}
            aria-label={`Consultar ${point.code}`}
          >
            {point.id === selectedId ? 'Punto seleccionado' : 'Consultar punto'}{' '}
            <span aria-hidden="true">→</span>
          </button>
        </article>
      ))}
    </div>
  );
}
