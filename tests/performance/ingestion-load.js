// 4.4.3 · Prueba de carga y estrés de la ingesta de mediciones (flujo crítico).
// Cada iteración envía una medición: Telemetry valida el punto en Registry,
// persiste y pide la evaluación a Alert Management (3 servicios + 3 bases).
//
//   docker run --rm -i -e BASE_URL=... -e ADMIN_PASSWORD=... -e PROFILE=load grafana/k6 run - < ingestion-load.js
import http from 'k6/http';
import { check } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:18080';
const PROFILE = __ENV.PROFILE || 'load';

const profiles = {
  // Carga esperada: 20 dispositivos que reportan sin pausa (~ cientos de lecturas/min).
  load: [
    { duration: '30s', target: 10 },
    { duration: '2m', target: 20 },
    { duration: '30s', target: 0 },
  ],
  // Estrés: triplica la carga para forzar el autoescalado y encontrar el límite.
  stress: [
    { duration: '30s', target: 20 },
    { duration: '1m', target: 40 },
    { duration: '2m', target: 60 },
    { duration: '1m', target: 0 },
  ],
};

// TARGET_IP permite apuntar a un Ingress por nombre de host sin DNS (p. ej. *.localhost).
const hostName = BASE_URL.replace(/^https?:\/\//, '').split(/[:/]/)[0];

export const options = {
  hosts: __ENV.TARGET_IP ? { [hostName]: __ENV.TARGET_IP } : {},
  scenarios: {
    ingestion: { executor: 'ramping-vus', startVUs: 0, stages: profiles[PROFILE], gracefulRampDown: '20s' },
  },
  thresholds: {
    // SLO del flujo crítico (sección 4.3): p95 < 500 ms y menos de 1 % de errores.
    'http_req_duration{name:ingest}': PROFILE === 'load' ? ['p(95)<500'] : ['p(95)<2000'],
    ingest_errors: ['rate<0.01'],
  },
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

const ingestErrors = new Rate('ingest_errors');
const evaluationCompleted = new Rate('evaluation_completed');
const ingestLatency = new Trend('ingest_latency', true);

export function setup() {
  const login = http.post(`${BASE_URL}/identity/api/v1/auth/login`,
    JSON.stringify({ username: __ENV.ADMIN_USER || 'admin', password: __ENV.ADMIN_PASSWORD }),
    { headers: { 'Content-Type': 'application/json' } });
  check(login, { 'login 200': (r) => r.status === 200 });
  const auth = { 'Content-Type': 'application/json', Authorization: `Bearer ${login.json('token')}` };
  const code = `K6-${Date.now().toString(36).toUpperCase()}`;
  const point = http.post(`${BASE_URL}/registry/api/v1/monitoring-points`,
    JSON.stringify({ code, name: 'Punto de carga k6', type: 'TRANSPORTE' }), { headers: auth });
  const pointId = point.json('id');
  http.put(`${BASE_URL}/alerts/api/alerts/rules/${pointId}`,
    JSON.stringify({ thresholdMin: 2, thresholdMax: 8 }), { headers: auth });
  return { auth, pointId };
}

export default function (data) {
  // 80 % dentro del rango (2–8 °C) y 20 % fuera, como en una ruta con desviaciones.
  const temperature = Math.random() < 0.8 ? 2 + Math.random() * 6 : 8.5 + Math.random() * 4;
  const res = http.post(`${BASE_URL}/telemetry/api/measurements`, JSON.stringify({
    monitoringPointId: data.pointId,
    temperature: Math.round(temperature * 100) / 100,
    timestamp: new Date().toISOString(),
  }), { headers: data.auth, tags: { name: 'ingest' } });
  const ok = check(res, { 'medición creada (201)': (r) => r.status === 201 });
  ingestErrors.add(!ok);
  ingestLatency.add(res.timings.duration);
  if (ok) evaluationCompleted.add(res.json('evaluationStatus') === 'COMPLETED');
}

export function handleSummary(data) {
  delete data.setup_data; // contiene el token de sesión: nunca se guarda en el reporte
  return { stdout: textSummary(data), [`/out/k6-${PROFILE}-summary.json`]: JSON.stringify(data, null, 2) };
}

function textSummary(data) {
  const d = data.metrics['http_req_duration{name:ingest}'] || data.metrics.http_req_duration;
  const v = d.values;
  return `\nPerfil ${PROFILE}: ${data.metrics.http_reqs.values.count} solicitudes, ` +
    `${data.metrics.http_reqs.values.rate.toFixed(1)} req/s\n` +
    `Latencia ingesta: avg ${v.avg.toFixed(0)} ms · p90 ${v['p(90)'].toFixed(0)} ms · p95 ${v['p(95)'].toFixed(0)} ms · p99 ${v['p(99)'].toFixed(0)} ms · max ${v.max.toFixed(0)} ms\n` +
    `Errores: ${(data.metrics.ingest_errors.values.rate * 100).toFixed(2)} % · ` +
    `Evaluaciones COMPLETED: ${((data.metrics.evaluation_completed?.values.rate || 0) * 100).toFixed(1)} %\n`;
}
