# Tercer incremento: cobertura de aceptación de todas las HU

**Goal:** Cubrir los criterios de las 36 HU: ejecutar los implementados y especificar el resto.
**Decisiones:**
- Solo se automatiza comportamiento existente; no se cambian APIs ni se crea producto nuevo.
- Lo no implementado queda en `tests/acceptance/specifications` con `@pendiente @no_implementado`.
- Escenarios que requieren reiniciar procesos con datos persistentes llevan `@requiere_postgres`;
  el perfil H2 los excluye explícitamente.
- CI añade Trivy (SARIF + gate HIGH/CRITICAL corregibles) y Sonar (quality gate, JaCoCo por CLI).

## Pasos
- [x] Ampliar HU-04 (tipos, campos vacíos, tipo no admitido).
- [x] Escenarios integrados HU-13, HU-14/HU-18, contrato Alerts, HU-10/CA-03, caída de Registry.
- [x] Especificaciones pendientes EP01–EP09 y validación por dry-run.
- [x] Verificación local PostgreSQL y H2; jobs Trivy y Sonar.
- [ ] Run remoto en Actions, secreto `SONAR_TOKEN` y actualización de pgjdbc (decisión del equipo).
