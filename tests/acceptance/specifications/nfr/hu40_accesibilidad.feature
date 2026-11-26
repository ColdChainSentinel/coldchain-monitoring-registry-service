# language: es
# Automatizado: tests/e2e/specs/accessibility.spec.ts (axe-core) y Lighthouse.
@NFR @HU40 @accesibilidad @automatizado
Característica: Accesibilidad de la interfaz web
  Como usuario con discapacidad visual o motriz quiero usar la plataforma con lector de
  pantalla y teclado para registrar y consultar la información térmica.

  Esquema del escenario: Sin infracciones WCAG 2.1 AA críticas ni serias
    Cuando se analiza la página "<página>" con axe-core en "<navegador>"
    Entonces no hay infracciones de impacto "critical" ni "serious"
    Ejemplos:
      | página     | navegador |
      | login      | chromium  |
      | resumen    | chromium  |
      | mediciones | chromium  |
      | alertas    | chromium  |
      | login      | firefox   |
      | resumen    | firefox   |
      | mediciones | firefox   |
      | alertas    | firefox   |

  Escenario: Puntaje de accesibilidad de Lighthouse
    Cuando Lighthouse audita la pantalla de inicio de sesión
    Entonces el puntaje de accesibilidad es de al menos 90
