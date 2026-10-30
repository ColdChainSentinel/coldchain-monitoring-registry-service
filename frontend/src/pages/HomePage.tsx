import { useCallback, useEffect, useRef, useState, type ReactNode } from 'react';
import { Icon, type IconName } from '../components/Icon';
import { EmptyState, Notice, ResourceFeedback } from '../components/Feedback';
import { ServiceHealth } from '../components/ServiceHealth';
import { PointForm } from '../features/monitoring/PointForm';
import { PointList } from '../features/monitoring/PointList';
import { MeasurementForm } from '../features/telemetry/MeasurementForm';
import { MeasurementTable } from '../features/telemetry/MeasurementTable';
import { EvaluationBadge, MeasurementReceipt } from '../features/telemetry/MeasurementStatus';
import { AlertTable } from '../features/alerts/AlertTable';
import { useResource, type Resource } from '../hooks/useResource';
import { formatDate, formatTemperature, pointTypeLabel } from '../lib/format';
import { monitoringApi } from '../services/monitoring';
import { telemetryApi } from '../services/telemetry';
import { alertsApi } from '../services/alerts';
import type { CreateMeasurement, CreatePoint, Measurement, MonitoringPoint } from '../types/domain';

const sections = {
  overview: { label: 'Resumen', title: 'Resumen de monitoreo', icon: 'grid' },
  monitoring: { label: 'Puntos', title: 'Puntos de monitoreo', icon: 'point' },
  telemetry: { label: 'Mediciones', title: 'Mediciones térmicas', icon: 'thermometer' },
  alerts: { label: 'Alertas', title: 'Alertas registradas', icon: 'bell' },
} satisfies Record<string, { label: string; title: string; icon: IconName }>;
type Section = keyof typeof sections;

function QueryCard({
  title,
  description,
  resource,
  retry,
  children,
  action,
}: {
  title: string;
  description: string;
  resource: Resource<unknown>;
  retry: () => void;
  children: ReactNode;
  action?: ReactNode;
}) {
  return (
    <section className="card data-card">
      <div className="card-heading">
        <div>
          <h2>{title}</h2>
          <p className="section-description">{description}</p>
        </div>
        {action}
      </div>
      <ResourceFeedback resource={resource} onRetry={retry} />
      {resource.data !== null && children}
      {resource.updatedAt && (
        <p className="data-footnote">Última consulta: {formatDate(resource.updatedAt)} · Hora local</p>
      )}
    </section>
  );
}

export function HomePage() {
  const [section, setSection] = useState<Section>('overview');
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [createdPoint, setCreatedPoint] = useState<MonitoringPoint | null>(null);
  const [lastMeasurement, setLastMeasurement] = useState<Measurement | null>(null);
  const [revision, setRevision] = useState(0);
  const heading = useRef<HTMLHeadingElement>(null);
  const points = useResource('points', monitoringApi.list, revision);
  const pointList = points.data || [];
  const selectedPoint =
    pointList.find((point) => point.id === selectedId) ||
    (createdPoint?.id === selectedId ? createdPoint : pointList[0]);
  const pointId = selectedPoint?.id || null;
  const loadMeasurements = useCallback(
    (signal: AbortSignal) => telemetryApi.list(pointId!, signal),
    [pointId],
  );
  const loadAlerts = useCallback((signal: AbortSignal) => alertsApi.list(pointId!, signal), [pointId]);
  const measurements = useResource(pointId ? `measurements:${pointId}` : null, loadMeasurements, revision);
  const alerts = useResource(pointId ? `alerts:${pointId}` : null, loadAlerts, revision);
  const alertsReady = alerts.data !== null && !alerts.loading && !alerts.error;
  const count = (resource: Resource<unknown[]>) =>
    !resource.loading && !resource.error && resource.data !== null ? resource.data.length : null;
  const pending =
    count(measurements) === null
      ? null
      : measurements.data!.filter((row) => row.evaluationStatus === 'PENDING').length;
  const latest = !measurements.loading && !measurements.error ? measurements.data?.at(-1) : null;
  const refresh = () => setRevision((value) => value + 1);
  const refreshing = points.loading || measurements.loading || alerts.loading;

  useEffect(() => {
    document.title = `${sections[section].label} · ColdChain Sentinel`;
    heading.current?.focus();
  }, [section]);

  async function createPoint(input: CreatePoint) {
    const point = await monitoringApi.create(input);
    setCreatedPoint(point);
    setSelectedId(point.id);
    points.update((rows) =>
      [...rows.filter((row) => row.id !== point.id), point].sort((a, b) => a.code.localeCompare(b.code)),
    );
    refresh();
    return point;
  }
  async function createMeasurement(input: CreateMeasurement) {
    const measurement = await telemetryApi.create(input);
    setLastMeasurement(measurement);
    refresh();
    return measurement;
  }

  const stats: { label: string; value: number | null; note: string; icon: IconName; tone: string }[] = [
    {
      label: 'Puntos registrados',
      value: count(points),
      note: 'Catálogo de Monitoring Registry',
      icon: 'point',
      tone: 'teal',
    },
    {
      label: 'Mediciones del punto',
      value: count(measurements),
      note: selectedPoint ? `Punto ${selectedPoint.code}` : 'Selecciona un punto',
      icon: 'thermometer',
      tone: 'blue',
    },
    {
      label: 'Alertas del punto',
      value: count(alerts),
      note: selectedPoint ? `Punto ${selectedPoint.code}` : 'Selecciona un punto',
      icon: 'bell',
      tone: 'rose',
    },
    {
      label: 'Evaluaciones pendientes',
      value: pending,
      note: 'PENDING almacenado en Telemetry',
      icon: 'clock',
      tone: 'amber',
    },
  ];

  return (
    <div className="app-shell">
      <a href="#contenido" className="skip-link">
        Saltar al contenido
      </a>
      <aside className="sidebar">
        <div className="brand">
          <span className="brand-icon">
            <Icon name="snow" />
          </span>
          <div>
            ColdChain<span>Sentinel</span>
          </div>
        </div>
        <div className="sidebar-divider" />
        <p className="nav-caption">ESPACIO DE MONITOREO</p>
        <nav aria-label="Navegación principal">
          {(Object.keys(sections) as Section[]).map((key) => (
            <button
              key={key}
              className={`nav-item ${section === key ? 'nav-item--active' : ''}`}
              aria-current={section === key ? 'page' : undefined}
              onClick={() => setSection(key)}
            >
              <Icon name={sections[key].icon} />
              <span>{sections[key].label}</span>
              {key === 'alerts' && count(alerts) !== null && (
                <span className="nav-count">{count(alerts)}</span>
              )}
            </button>
          ))}
        </nav>
        <div className="sidebar-footer">
          <Icon name="snow" />
          <strong>Cadena de frío, datos claros.</strong>
          <p>MVP académico · Agile Software Factories</p>
        </div>
      </aside>
      <main id="contenido" className="workspace" tabIndex={-1}>
        <header className="page-header">
          <div>
            <span className="eyebrow">COLDCHAIN SENTINEL / {sections[section].label.toUpperCase()}</span>
            <h1 ref={heading} tabIndex={-1}>
              {sections[section].title}
            </h1>
            <p>Consulta y registra datos reales de tu cadena de frío.</p>
          </div>
          <button className="button button--secondary" disabled={refreshing} onClick={refresh}>
            <Icon name="refresh" />
            Actualizar datos
          </button>
        </header>
        <ServiceHealth revision={revision} />
        <section className="context-bar" aria-label="Selección del punto de monitoreo">
          <span className="context-icon">
            <Icon name="point" />
          </span>
          <div className="context-field">
            <label htmlFor="active-point">Punto de monitoreo seleccionado</label>
            <select
              id="active-point"
              value={pointId || ''}
              disabled={!pointList.length && !createdPoint}
              onChange={(e) => setSelectedId(e.target.value)}
            >
              {!pointList.length && !createdPoint && (
                <option value="">
                  {points.loading
                    ? 'Cargando puntos…'
                    : points.error
                      ? 'Catálogo no disponible'
                      : 'Sin puntos registrados'}
                </option>
              )}
              {pointList.map((point) => (
                <option value={point.id} key={point.id}>
                  {point.code} · {point.name}
                </option>
              ))}
              {createdPoint && !pointList.some((point) => point.id === createdPoint.id) && (
                <option value={createdPoint.id}>
                  {createdPoint.code} · {createdPoint.name}
                </option>
              )}
            </select>
          </div>
          <p className="context-note">
            Las mediciones y alertas se consultan por punto.
            <br />
            <strong>Sin estadísticas globales estimadas.</strong>
          </p>
        </section>
        <ResourceFeedback resource={points} onRetry={points.reload} />

        {section === 'overview' && (
          <>
            <div className="stats-grid">
              {stats.map((stat) => (
                <article className="stat-card" key={stat.label} aria-label={stat.label}>
                  <div className="stat-top">
                    <span>{stat.label}</span>
                    <span className={`stat-icon stat-icon--${stat.tone}`}>
                      <Icon name={stat.icon} />
                    </span>
                  </div>
                  <strong className="stat-number">{stat.value ?? '—'}</strong>
                  <p>{stat.value === null && pointId ? 'Consulta pendiente o no disponible' : stat.note}</p>
                </article>
              ))}
            </div>
            {selectedPoint ? (
              <>
                <section className="point-hero">
                  <div>
                    <span className="eyebrow">
                      PUNTO EN CONSULTA · {pointTypeLabel(selectedPoint.type).toUpperCase()}
                    </span>
                    <h2>{selectedPoint.name}</h2>
                    <p>
                      {selectedPoint.code} <span>·</span> Datos del punto seleccionado, sin umbrales asumidos.
                    </p>
                  </div>
                  <div className="hero-reading">
                    <span>Última medición disponible</span>
                    <strong>
                      {latest ? formatTemperature(latest.temperature) : 'Sin lectura confirmada'}
                    </strong>
                    {latest && (
                      <>
                        <time dateTime={latest.timestamp}>{formatDate(latest.timestamp)}</time>
                        <EvaluationBadge status={latest.evaluationStatus} />
                      </>
                    )}
                  </div>
                </section>
                <div className="overview-grid">
                  <QueryCard
                    title="Mediciones recientes"
                    description="Últimas cinco lecturas · Más recientes primero"
                    resource={measurements}
                    retry={measurements.reload}
                    action={
                      <button className="text-button" onClick={() => setSection('telemetry')}>
                        Ver mediciones <Icon name="arrow" />
                      </button>
                    }
                  >
                    <MeasurementTable
                      measurements={measurements.data?.slice(-5) || []}
                      alerts={alerts.data}
                      alertsReady={!!alertsReady}
                    />
                  </QueryCard>
                  <QueryCard
                    title="Alertas recientes"
                    description="Últimas tres alertas del punto"
                    resource={alerts}
                    retry={alerts.reload}
                    action={
                      <button className="text-button" onClick={() => setSection('alerts')}>
                        Ver alertas <Icon name="arrow" />
                      </button>
                    }
                  >
                    {!alerts.data?.length ? (
                      <EmptyState title="Sin alertas registradas" icon="bell">
                        No implica que las evaluaciones pendientes estén dentro del rango.
                      </EmptyState>
                    ) : (
                      <div className="recent-alerts">
                        {alerts.data
                          .slice(-3)
                          .reverse()
                          .map((alert) => (
                            <article className="recent-alert" key={alert.id}>
                              <div>
                                <span
                                  className={`alert-mark ${alert.type === 'LOW_TEMPERATURE' ? 'alert-mark--cold' : ''}`}
                                >
                                  <Icon name="thermometer" />
                                </span>
                                <strong>
                                  {alert.type === 'LOW_TEMPERATURE'
                                    ? 'Temperatura baja'
                                    : alert.type === 'HIGH_TEMPERATURE'
                                      ? 'Temperatura alta'
                                      : 'Alerta registrada'}
                                </strong>
                                <span className="badge badge--warning">
                                  {alert.status === 'OPEN' ? 'OPEN' : alert.status}
                                </span>
                              </div>
                              <strong className="recent-temperature">
                                {formatTemperature(alert.temperature)}
                              </strong>
                              <p>
                                Límites: {formatTemperature(alert.thresholdMin)} —{' '}
                                {formatTemperature(alert.thresholdMax)}
                              </p>
                              <time dateTime={alert.createdAt}>{formatDate(alert.createdAt)}</time>
                            </article>
                          ))}
                      </div>
                    )}
                  </QueryCard>
                </div>
              </>
            ) : (
              !points.loading &&
              !points.error && (
                <section className="card">
                  <EmptyState title="Tu primer punto, el inicio del monitoreo" icon="point">
                    Registra un punto para habilitar las mediciones y consultar sus alertas.
                  </EmptyState>
                  <div className="empty-action">
                    <button className="button button--primary" onClick={() => setSection('monitoring')}>
                      <Icon name="plus" />
                      Registrar primer punto
                    </button>
                  </div>
                </section>
              )
            )}
            <p className="page-note">
              <Icon name="info" />
              Los indicadores cuentan únicamente registros devueltos por las APIs. Actualización manual; no
              son métricas en tiempo real.
            </p>
          </>
        )}

        {section === 'monitoring' && (
          <div className="feature-grid">
            <section className="card data-card">
              <div className="card-heading">
                <div>
                  <span className="eyebrow">CATÁLOGO</span>
                  <h2>Puntos registrados</h2>
                </div>
                <Icon name="point" />
              </div>
              <p className="section-description">
                Selecciona un punto para consultar su historial. No hay edición ni eliminación en este MVP.
              </p>
              {points.data !== null && (
                <PointList points={points.data} selectedId={pointId} onSelect={setSelectedId} />
              )}
            </section>
            <PointForm onCreate={createPoint} />
          </div>
        )}

        {section === 'telemetry' &&
          (selectedPoint ? (
            <>
              {lastMeasurement?.monitoringPointId === pointId && (
                <MeasurementReceipt measurement={lastMeasurement} />
              )}
              <div className="feature-grid">
                <QueryCard
                  title="Historial de mediciones"
                  description="Punto seleccionado · Más recientes primero · Horas locales"
                  resource={measurements}
                  retry={measurements.reload}
                >
                  <MeasurementTable
                    measurements={measurements.data || []}
                    alerts={alerts.data}
                    alertsReady={!!alertsReady}
                  />
                </QueryCard>
                <MeasurementForm key={pointId} point={selectedPoint} onCreate={createMeasurement} />
              </div>
              {!!alerts.error && (
                <Notice tone="warning">
                  Las mediciones pueden consultarse, pero no se pudo confirmar su asociación con las alertas.
                  <button className="text-button" onClick={alerts.reload}>
                    Reintentar consulta de alertas
                  </button>
                </Notice>
              )}
              <p className="page-note">
                <Icon name="info" />
                La evaluación almacenada y la alerta asociada son datos distintos. Un reintento externo puede
                crear una alerta sin actualizar PENDING en Telemetry.
              </p>
            </>
          ) : (
            <section className="card">
              <EmptyState title="Selecciona o registra un punto" icon="point">
                Necesitas un UUID real de Monitoring Registry para introducir una medición.
              </EmptyState>
            </section>
          ))}

        {section === 'alerts' &&
          (selectedPoint ? (
            <>
              <Notice>
                Los límites mostrados son los snapshots guardados con cada alerta. Las reglas se configuran
                externamente por UUID; no se modifican desde esta interfaz.
              </Notice>
              <QueryCard
                title="Historial de alertas"
                description="Punto seleccionado · Más recientes primero · Fecha de creación de la alerta"
                resource={alerts}
                retry={alerts.reload}
              >
                <AlertTable alerts={alerts.data || []} />
              </QueryCard>
              {(pending || 0) > 0 && (
                <Notice tone="warning">
                  Telemetry informa {pending} evaluación(es) PENDING en este punto. Una lista sin alertas no
                  equivale a NO_ALERT.
                </Notice>
              )}
            </>
          ) : (
            <section className="card">
              <EmptyState title="Selecciona o registra un punto" icon="point">
                Las alertas se consultan por el identificador de un punto existente.
              </EmptyState>
            </section>
          ))}
        <footer className="workspace-footer">
          <span>ColdChain Sentinel</span>
          <span>MVP académico · Datos de APIs reales</span>
        </footer>
      </main>
    </div>
  );
}
