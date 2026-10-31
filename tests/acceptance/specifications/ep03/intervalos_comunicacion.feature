# language: es
# Diseño de 2.6. Sin implementación ni step definitions; no se ejecuta en HU-04.
@pendiente @no_implementado
@EP03 @HU11
Característica: Configuración del tiempo máximo sin datos
  Antecedentes:
    Dado un supervisor autenticado con permisos sobre el dispositivo "SEN-01"
    Y que el dispositivo está activo

  @CA01
  Escenario: Guardar un intervalo positivo
    Cuando configura el tiempo máximo sin datos en 5 minutos
    Entonces se guarda el intervalo de 5 minutos para el dispositivo

  @CA02
  Esquema del escenario: Rechazar un intervalo inválido
    Cuando intenta configurar el valor "<valor>" en minutos
    Entonces se rechaza el cambio
    Y se conserva la configuración anterior
    Ejemplos:
      | valor |
      | nulo  |
      | 0     |
      | -1    |

  @CA03
  Esquema del escenario: Detectar una interrupción de comunicación
    Dado un intervalo máximo de 5 minutos
    Y que el instante de referencia corresponde a "<referencia>"
    Cuando transcurren 6 minutos sin recibir una medición válida
    Entonces el dispositivo se identifica como "Sin comunicación"
    Ejemplos:
      | referencia                  |
      | última recepción válida     |
      | activación sin transmisiones |
