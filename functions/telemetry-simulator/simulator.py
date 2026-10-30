"""Emisor de pruebas: usa el contrato de Telemetry, nunca evalúa reglas."""

import json
import logging
import math
import os
from dataclasses import dataclass
from datetime import datetime, timezone
from urllib.error import HTTPError, URLError
from urllib.parse import urlsplit
from urllib.request import Request, build_opener, HTTPRedirectHandler
from uuid import UUID


class NoRedirect(HTTPRedirectHandler):
    """No reenviar una medición o sus credenciales a otro destino."""

    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


@dataclass(frozen=True)
class Settings:
    base_url: str
    point_id: str
    temperature: float
    timeout: float
    dry_run: bool

    @classmethod
    def from_env(cls, env=None):
        env = os.environ if env is None else env
        base_url = env.get("TELEMETRY_BASE_URL", "http://localhost:8081").rstrip("/")
        url = urlsplit(base_url)
        if (url.scheme not in {"http", "https"} or not url.hostname
                or url.username or url.password or url.query or url.fragment
                or url.path not in {"", "/"}):
            raise ValueError("TELEMETRY_BASE_URL debe ser un origen HTTP(S) sin credenciales")
        point_id = str(UUID(env.get("MONITORING_POINT_ID", "")))
        temperature = float(env.get("SIMULATED_TEMPERATURE", "5"))
        timeout = float(env.get("HTTP_TIMEOUT_SECONDS", "5"))
        if not math.isfinite(temperature):
            raise ValueError("SIMULATED_TEMPERATURE debe ser finita")
        if not math.isfinite(timeout) or not 0 < timeout <= 30:
            raise ValueError("HTTP_TIMEOUT_SECONDS debe estar entre 0 y 30 segundos")
        dry_run = env.get("SIMULATOR_DRY_RUN", "true").lower()
        if dry_run not in {"true", "false"}:
            raise ValueError("SIMULATOR_DRY_RUN debe ser true o false")
        return cls(base_url, point_id, temperature, timeout, dry_run == "true")


def run_once(settings, *, now=None, opener=None):
    instant = now or datetime.now(timezone.utc)
    if instant.tzinfo is None or instant.utcoffset() is None:
        raise ValueError("La fecha debe incluir zona horaria")
    payload = {
        "monitoringPointId": settings.point_id,
        "temperature": settings.temperature,
        "timestamp": instant.astimezone(timezone.utc).isoformat().replace("+00:00", "Z"),
    }
    if settings.dry_run:
        logging.info("simulator dry_run=true point=%s temperature=%s timestamp=%s",
                     payload["monitoringPointId"], payload["temperature"], payload["timestamp"])
        return {"mode": "DRY_RUN", "payload": payload}

    request = Request(settings.base_url + "/api/measurements",
                      data=json.dumps(payload, allow_nan=False).encode("utf-8"),
                      headers={"Content-Type": "application/json", "Accept": "application/json"},
                      method="POST")
    # Un único intento: la ingesta todavía no tiene clave de idempotencia.
    sender = opener or build_opener(NoRedirect())
    try:
        with sender.open(request, timeout=settings.timeout) as response:
            if response.status != 201:
                raise RuntimeError("Telemetry devolvió un estado distinto de 201")
            result = json.load(response)
    except HTTPError as exc:
        # No registrar cuerpo, URL ni credenciales del destino.
        code = exc.code
        exc.close()
        raise RuntimeError(f"Telemetry HTTP {code}; sin reintento automático") from None
    except (URLError, TimeoutError, OSError):
        raise RuntimeError("Fallo de transporte; resultado desconocido, no reenviar automáticamente") from None

    if (not isinstance(result, dict) or not isinstance(result.get("id"), str)
            or result.get("evaluationStatus") not in {"COMPLETED", "PENDING", "RULE_NOT_CONFIGURED"}):
        raise RuntimeError("Respuesta de Telemetry inválida; no reenviar automáticamente")
    try:
        UUID(result["id"])
    except ValueError:
        raise RuntimeError("Identificador de Telemetry inválido; no reenviar automáticamente") from None
    logging.info("simulator persisted measurement_id=%s evaluation_status=%s",
                 result["id"], result["evaluationStatus"])
    return result


if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO)
    print(json.dumps(run_once(Settings.from_env()), indent=2))
