# language: es
# Diseño pendiente. Sin implementación ni step definitions; no se ejecuta.
@pendiente @no_implementado
@EP01 @HU01
Característica: Inicio y cierre de sesión

  @CA01
  Escenario: Iniciar sesión con una cuenta activa
    Dado una cuenta activa con identificador "supervisor01"
    Cuando inicia sesión con credenciales correctas
    Entonces se muestra un mensaje de inicio de sesión exitoso

  @CA02
  Esquema del escenario: Rechazar el acceso sin revelar el dato incorrecto (<caso>)
    Dado una cuenta "<estado>" con identificador "supervisor01"
    Cuando inicia sesión con <credenciales>
    Entonces se rechaza el acceso
    Y el mensaje es idéntico en todos los casos y no indica qué dato es incorrecto
    Ejemplos:
      | caso                   | estado   | credenciales                  |
      | contraseña incorrecta  | activa   | contraseña incorrecta         |
      | usuario inexistente    | activa   | un identificador inexistente  |
      | cuenta inactiva        | inactiva | credenciales correctas        |

  @CA03
  Escenario: Exigir nueva autenticación después de cerrar sesión
    Dado un usuario con sesión iniciada
    Cuando cierra sesión
    Y intenta acceder a una función protegida con la sesión anterior
    Entonces se exige una nueva autenticación
