# language: es
@EP02 @HU04
Característica: Registro de puntos de monitoreo
  Como supervisor quiero registrar puntos para identificar dónde se controla la cadena de frío.

  @CA01
  Escenario: Registrar un punto válido y consultarlo
    Dado que el registro de puntos está vacío
    Cuando registro el siguiente punto:
      | code   | name     | type           |
      | CAM-01 | Cámara 1 | ALMACENAMIENTO |
    Entonces la API responde con estado 201
    Y el punto creado se puede consultar y aparece en el listado con sus datos
    Y existen 1 puntos registrados

  @CA01
  Esquema del escenario: Registrar un punto de tipo <tipo>
    Dado que el registro de puntos está vacío
    Cuando registro el siguiente punto:
      | code     | name     | type     |
      | <codigo> | <nombre> | <tipo>   |
    Entonces la API responde con estado 201
    Y el punto creado se puede consultar y aparece en el listado con sus datos
    Ejemplos:
      | codigo | nombre               | tipo           |
      | ALM-01 | Almacén central      | ALMACENAMIENTO |
      | UNI-07 | Unidad refrigerada 7 | TRANSPORTE     |

  @CA02
  Escenario: Rechazar un código duplicado sin modificar el punto original
    Dado que existe el siguiente punto:
      | code   | name     | type           |
      | CAM-01 | Cámara 1 | ALMACENAMIENTO |
    Cuando registro el siguiente punto:
      | code   | name     | type       |
      | cam-01 | Otro     | TRANSPORTE |
    Entonces la API responde con estado 409
    Y se informa "Ya existe un punto con ese código"
    Y el listado conserva únicamente el punto original

  @CA03
  Escenario: Rechazar datos obligatorios ausentes sin guardar un punto
    Dado que el registro de puntos está vacío
    Cuando envío un registro sin datos
    Entonces la API responde con estado 400
    Y se identifican los siguientes campos obligatorios:
      | campo |
      | code  |
      | name  |
      | type  |
    Y existen 0 puntos registrados

  @CA03
  Esquema del escenario: Rechazar un registro sin <campo>
    Dado que el registro de puntos está vacío
    Cuando registro el siguiente punto:
      | code     | name     | type           |
      | <codigo> | <nombre> | ALMACENAMIENTO |
    Entonces la API responde con estado 400
    Y se identifican los siguientes campos obligatorios:
      | campo   |
      | <campo> |
    Y existen 0 puntos registrados
    Ejemplos:
      | codigo | nombre   | campo |
      |        | Cámara 1 | code  |
      | CAM-01 |          | name  |

  @CA01 @contrato
  Escenario: Rechazar un tipo de punto no admitido
    Dado que el registro de puntos está vacío
    Cuando registro el siguiente punto:
      | code   | name     | type       |
      | CAM-01 | Cámara 1 | CONGELADOR |
    Entonces la API responde con estado 400
    Y se informa "JSON, tipo de punto o identificador inválido. Tipos: ALMACENAMIENTO, TRANSPORTE"
    Y existen 0 puntos registrados
