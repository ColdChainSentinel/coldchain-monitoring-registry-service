import { defineConfig, devices } from '@playwright/test';

/**
 * E2E contra un ambiente desplegado (BASE_URL). Las especificaciones generales
 * corren en Chromium y Firefox (browser-agnostic); las que dependen de capacidades
 * de un motor se etiquetan @chromium y solo corren ahí (browser-specific).
 */
export default defineConfig({
  testDir: './specs',
  timeout: 60_000,
  expect: { timeout: 15_000 },
  fullyParallel: false,
  workers: 1,
  retries: process.env.CI ? 1 : 0,
  reporter: [
    ['list'],
    ['html', { outputFolder: 'reports/html', open: 'never' }],
    ['junit', { outputFile: 'reports/junit.xml' }],
    ['json', { outputFile: 'reports/results.json' }],
  ],
  use: {
    baseURL: process.env.BASE_URL ?? 'http://localhost:18080',
    locale: 'es-PE',
    timezoneId: 'America/Lima',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
    { name: 'firefox', use: { ...devices['Desktop Firefox'] }, grepInvert: /@chromium/ },
    { name: 'mobile-chromium', use: { ...devices['Pixel 7'] }, grep: /@chromium/ },
  ],
});
