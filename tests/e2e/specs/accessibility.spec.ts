import AxeBuilder from '@axe-core/playwright';
import { expect, test } from '@playwright/test';
import { goTo, loginAsAdmin } from './helpers';
import { writeFileSync, mkdirSync } from 'node:fs';

// 4.4.5 · Accesibilidad WCAG 2.1 AA con axe-core. Falla ante infracciones
// críticas o serias; las moderadas y menores se registran en el reporte.
const pages: { name: string; open: (page: import('@playwright/test').Page) => Promise<void> }[] = [
  { name: 'login', open: async (page) => { await page.goto('/'); } },
  { name: 'resumen', open: async (page) => { await loginAsAdmin(page); } },
  { name: 'mediciones', open: async (page) => { await loginAsAdmin(page); await goTo(page, 'Mediciones'); } },
  { name: 'alertas', open: async (page) => { await loginAsAdmin(page); await goTo(page, 'Alertas'); } },
];

for (const target of pages) {
  test(`WCAG 2.1 AA · ${target.name}`, async ({ page, browserName }, info) => {
    await target.open(page);
    await page.waitForLoadState('networkidle');
    const results = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa']).analyze();
    mkdirSync('reports/axe', { recursive: true });
    writeFileSync(`reports/axe/${target.name}-${browserName}-${info.project.name}.json`, JSON.stringify(results.violations, null, 2));
    const blocking = results.violations.filter((v) => v.impact === 'critical' || v.impact === 'serious');
    expect(blocking.map((v) => `${v.id}: ${v.help} (${v.nodes.length})`)).toEqual([]);
  });
}
