# language: es
# Diseño pendiente. Sin implementación ni step definitions; no se ejecuta.
@pendiente @no_implementado
@EP09 @HU35
Característica: Exportación CSV de mediciones

  Antecedentes:
    Dado un supervisor con ámbito sobre "ALM-01" y sin acceso a "ALM-03"

  @CA01
  Escenario: Exportar todos los registros aunque el listado esté paginado
    Dado una consulta con 250 mediciones mostradas en páginas de 50
    Cuando exporta la consulta
    Entonces se genera un archivo CSV con los 250 registros

  @CA02
  Escenario: Incluir las columnas requeridas
    Cuando exporta una consulta con resultados
    Entonces cada fila incluye dispositivo, punto, temperatura, unidad, fecha y hora con zona horaria y límites aplicables cuando existan

  @CA03
  Escenario: Limitar el archivo a su ámbito
    Cuando exporta una consulta de "ALM-01" y "ALM-03"
    Entonces el archivo contiene solo mediciones de "ALM-01"
