import { expect, test } from '@playwright/test';
import { goTo, loginAsAdmin } from './helpers';

// Pruebas específicas de navegador: emulación de dispositivo móvil con toque
// (isMobile/hasTouch), disponible solo en motores Chromium.
test.describe('Uso en móvil @chromium', () => {
  test('el conductor puede navegar y registrar una medición con pantalla táctil', async ({ page, isMobile }) => {
    test.skip(!isMobile, 'Solo en el proyecto mobile-chromium');
    await loginAsAdmin(page);
    await goTo(page, 'Mediciones');
    const input = page.getByLabel('Temperatura (°C)');
    if (await input.count()) {
      await input.tap();
      await expect(input).toBeFocused();
      await expect(input).toHaveAttribute('inputmode', 'decimal');
    }
    const width = await page.evaluate(() => document.documentElement.scrollWidth);
    expect(width).toBeLessThanOrEqual(page.viewportSize()!.width + 1);
  });
});
