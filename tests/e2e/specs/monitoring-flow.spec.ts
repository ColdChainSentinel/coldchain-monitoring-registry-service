import { expect, test } from '@playwright/test';
import { goTo, loginAsAdmin, uniqueCode } from './helpers';

// Flujo de negocio principal: HU-04 → HU-09 → HU-12 → HU-19.
test.describe('Flujo de monitoreo térmico', () => {
  test('registra un punto, configura su rango y genera una alerta por desviación', async ({ page }) => {
    const code = uniqueCode('E2E');
    await loginAsAdmin(page);

    await test.step('HU-04 · registrar punto de monitoreo', async () => {
      await goTo(page, 'Puntos');
      await page.getByLabel('Código del punto').fill(code);
      await page.getByLabel('Nombre').fill('Cámara E2E');
      await page.getByLabel('Tipo').selectOption('ALMACENAMIENTO');
      await page.getByRole('button', { name: 'Registrar punto' }).click();
      await expect(page.getByLabel('Punto de monitoreo seleccionado')).toContainText(code);
    });

    await test.step('HU-09 · configurar rango térmico de 2 a 8 °C', async () => {
      await goTo(page, 'Alertas');
      await page.getByLabel('Mínima (°C)').fill('2');
      await page.getByLabel('Máxima (°C)').fill('8');
      await page.getByRole('button', { name: 'Guardar rango' }).click();
      await expect(page.getByText(/Rango térmico guardado/)).toBeVisible();
    });

    await test.step('Validación · rechaza una temperatura fuera del rango operativo', async () => {
      await goTo(page, 'Mediciones');
      await page.getByLabel('Temperatura (°C)').fill('85');
      await page.getByRole('button', { name: 'Guardar medición' }).click();
      await expect(page.getByText('La temperatura debe estar entre -40 °C y 50 °C.')).toBeVisible();
    });

    await test.step('HU-12 · registrar una medición de 10 °C', async () => {
      await page.getByLabel('Temperatura (°C)').fill('10');
      await page.getByRole('button', { name: 'Guardar medición' }).click();
      await expect(page.getByLabel('Temperatura (°C)')).toHaveValue('');
    });

    await test.step('HU-19 · la desviación aparece como alerta de temperatura alta', async () => {
      await goTo(page, 'Alertas');
      const table = page.getByLabel(/Tabla de alertas/);
      await expect(table.getByText('Temperatura alta')).toBeVisible();
      await expect(table).toContainText('10');
    });
  });
});
