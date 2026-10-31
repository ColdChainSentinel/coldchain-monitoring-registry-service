# language: es
# Diseño de 2.6. Sin implementación ni step definitions; no se ejecuta en HU-04.
@pendiente @no_implementado
@EP03 @HU09
Característica: Configuración de rangos de temperatura
  Antecedentes:
    Dado un supervisor autenticado con permisos sobre el punto "ALM-01"

  @CA01
  Escenario: Guardar un rango válido
    Cuando configura el rango con mínimo 2 y máximo 8 en grados Celsius
    Entonces el rango del punto queda guardado
    Y la consulta muestra mínimo 2, máximo 8 y unidad "°C"

  @CA02
  Esquema del escenario: Rechazar un rango inválido
    Cuando intenta configurar mínimo <minimo> y máximo <maximo>
    Entonces se rechaza la configuración
    Y el rango previamente guardado permanece sin cambios
    Ejemplos:
      | minimo | maximo |
      | 8      | 2      |
      | 5      | 5      |

  @CA03
  Esquema del escenario: Incluir los límites del rango
    Dado un rango vigente de 2 a 8 grados Celsius
    Cuando se evalúa una medición válida de <temperatura> grados Celsius
    Entonces la medición se considera dentro del rango
    Ejemplos:
      | temperatura |
      | 2           |
      | 8           |
