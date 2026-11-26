import { expect, test } from '@playwright/test';
import { login, loginAsAdmin } from './helpers';

// HU-01: iniciar y cerrar sesión.
test.describe('HU-01 · Acceso a la plataforma', () => {
  test('rechaza credenciales incorrectas sin revelar qué dato falló (CA-02)', async ({ page }) => {
    await login(page, 'admin', 'contraseña-incorrecta');
    const alert = page.getByRole('alert');
    await expect(alert).toBeVisible();
    await expect(alert).not.toContainText(/usuario no existe|contraseña incorrecta/i);
    await expect(page.getByRole('button', { name: 'Cerrar sesión' })).toHaveCount(0);
  });

  test('inicia sesión y al cerrarla exige autenticarse de nuevo (CA-01, CA-03)', async ({ page }) => {
    await loginAsAdmin(page);
    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Resumen de monitoreo');
    await page.getByRole('button', { name: 'Cerrar sesión' }).click();
    await expect(page.getByRole('heading', { name: 'Iniciar sesión' })).toBeVisible();
    await page.reload();
    await expect(page.getByRole('heading', { name: 'Iniciar sesión' })).toBeVisible();
  });
});
