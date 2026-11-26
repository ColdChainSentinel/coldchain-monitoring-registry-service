# language: es
# Automatizado: tests/security/owasp-top10-checks.sh, ZAP baseline (zap-rules.tsv)
# y Trivy en el pipeline DevSecOps.
@NFR @HU38 @seguridad @automatizado
Característica: Seguridad de la aplicación desplegada
  Como administrador quiero que la plataforma resista los ataques más comunes de la web
  para proteger los datos de la cadena de frío.

  Esquema del escenario: Controles del OWASP Top 10
    Cuando se ejecuta la verificación "<control>" contra el ambiente
    Entonces el resultado esperado es "<esperado>"
    Ejemplos:
      | categoría | control                                         | esperado |
      | A01       | API sin token                                   | 401      |
      | A01       | Token inválido                                  | 401      |
      | A03       | Inyección SQL en el código del punto            | 400      |
      | A04       | Temperatura fuera de rango enviada a la API     | 400      |
      | A05       | Actuator /env expuesto por el proxy             | 404      |
      | A05       | Cabecera X-Frame-Options                        | DENY     |
      | A07       | Sesión usada después de cerrar sesión           | 401      |
      | A09       | Error sin stack trace                           | sin traza|

  Escenario: Escaneo dinámico sin alertas bloqueantes
    Cuando ZAP ejecuta el escaneo baseline sobre el frontend desplegado
    Entonces no hay alertas de reglas marcadas como FAIL
