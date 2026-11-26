#!/usr/bin/env sh
# Verificaciones dirigidas del OWASP Top 10 (2021) contra un ambiente desplegado.
# Complementan el escaneo automático de ZAP con casos propios del dominio.
# Uso: BASE_URL=http://host:puerto ADMIN_PASSWORD=... sh owasp-top10-checks.sh
set -u
B="${BASE_URL:?}"
pass=0; fail=0
result() { if [ "$2" = "$3" ]; then pass=$((pass+1)); printf 'PASS  %-6s %s (HTTP %s)\n' "$1" "$4" "$2"; else fail=$((fail+1)); printf 'FAIL  %-6s %s (esperado %s, obtenido %s)\n' "$1" "$4" "$3" "$2"; fi; }
code() { curl -s -o /dev/null -w '%{http_code}' "$@"; }
json='Content-Type: application/json'

token=$(curl -s -H "$json" -d "{\"username\":\"admin\",\"password\":\"${ADMIN_PASSWORD:?}\"}" "$B/identity/api/v1/auth/login" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
auth="Authorization: Bearer $token"

# A01 Broken Access Control
result A01 "$(code "$B/registry/api/v1/monitoring-points")" 401 "API sin token rechazada"
result A01 "$(code -H 'Authorization: Bearer token-falso' "$B/telemetry/api/measurements?monitoringPointId=00000000-0000-4000-8000-000000000000")" 401 "Token inválido rechazado"
result A01 "$(code -H "$auth" "$B/identity/api/v1/users/00000000-0000-4000-8000-000000000000/permissions" -X PUT -H "$json" -d '{}')" 400 "Validación antes de modificar permisos"
# A02 Cryptographic Failures: contraseñas no viajan en la URL ni se devuelven
result A02 "$(curl -s -H "$auth" "$B/identity/api/v1/users" | grep -ci 'password')" 0 "La API de usuarios no expone contraseñas ni hashes"
# A03 Injection
result A03 "$(code -H "$auth" "$B/registry/api/v1/monitoring-points" -H "$json" -d "{\"code\":\"X' OR '1'='1\",\"name\":\"a\",\"type\":\"ALMACENAMIENTO\"}")" 400 "Inyección SQL en código de punto rechazada por validación"
result A03 "$(code -H "$auth" "$B/telemetry/api/measurements?monitoringPointId=1%27%20OR%201=1--")" 400 "Inyección en parámetro UUID rechazada"
# A04 Insecure Design: reglas de negocio en el servidor, no solo en la interfaz
result A04 "$(code -H "$auth" "$B/telemetry/api/measurements" -H "$json" -d '{"monitoringPointId":"00000000-0000-4000-8000-000000000000","temperature":999,"timestamp":"2026-11-26T10:00:00Z"}')" 400 "Temperatura fuera de rango rechazada por la API"
# A05 Security Misconfiguration
result A05 "$(code "$B/registry/actuator/env")" 404 "Actuator /env no expuesto"
result A05 "$(code "$B/telemetry/actuator/prometheus")" 404 "Métricas internas no expuestas por el proxy"
result A05 "$(curl -sI "$B/" | grep -ci '^x-frame-options: deny')" 1 "Cabecera X-Frame-Options"
result A05 "$(curl -sI "$B/" | grep -ci '^content-security-policy:')" 1 "Cabecera Content-Security-Policy"
result A05 "$(curl -sI "$B/" | grep -ci '^server: nginx/')" 0 "Versión del servidor oculta"
# A07 Identification and Authentication Failures
result A07 "$(code -H "$json" -d '{"username":"admin","password":"incorrecta"}' "$B/identity/api/v1/auth/login")" 401 "Credenciales incorrectas rechazadas"
curl -s -o /dev/null -H "$auth" -X POST "$B/identity/api/v1/auth/logout"
result A07 "$(code -H "$auth" "$B/registry/api/v1/monitoring-points")" 401 "La sesión cerrada deja de ser válida"
# A09 Security Logging: errores sin trazas internas
result A09 "$(curl -s -H "$json" -d '{' "$B/identity/api/v1/auth/login" | grep -ci 'exception\|at pe.edu')" 0 "Errores sin stack trace"

printf '\nResultado: %s PASS, %s FAIL\n' "$pass" "$fail"
[ "$fail" -eq 0 ]
