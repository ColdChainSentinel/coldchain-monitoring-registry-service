# language: es
# Diseño pendiente. Sin implementación ni step definitions; no se ejecuta.
@pendiente @no_implementado
@EP01 @HU02
Característica: Asignación de roles y niveles de acceso

  Antecedentes:
    Dado un administrador autenticado
    Y los puntos de monitoreo "ALM-01" y "ALM-02"

  @CA01
  Escenario: Guardar y mostrar los permisos efectivos
    Cuando asigna al usuario "operador01" el rol "Responsable de almacén" sobre el punto "ALM-01"
    Entonces se guardan los permisos
    Y los permisos efectivos de "operador01" muestran el rol y el punto "ALM-01"

  @CA02
  Esquema del escenario: Rechazar <operación> fuera de los permisos
    Dado el usuario "operador01" con permisos solo sobre el punto "ALM-01"
    Cuando solicita <operación> del punto "ALM-02" mediante un enlace directo
    Entonces la solicitud se rechaza en el servidor
    Y no se devuelve información del punto "ALM-02"
    Ejemplos:
      | operación             |
      | la consulta           |
      | la exportación        |
      | la configuración      |

  @CA03
  Escenario: Aplicar el cambio de permisos en la siguiente solicitud
    Dado el usuario "operador01" con sesión iniciada y permisos sobre el punto "ALM-01"
    Cuando el administrador retira su permiso sobre "ALM-01"
    Entonces la siguiente solicitud de "operador01" sobre "ALM-01" se rechaza sin cerrar su sesión
