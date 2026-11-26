import { expect, type Page } from '@playwright/test';

export const ADMIN_USER = process.env.E2E_USERNAME ?? 'admin';
export const ADMIN_PASSWORD = process.env.E2E_PASSWORD ?? '';

export async function login(page: Page, username = ADMIN_USER, password = ADMIN_PASSWORD) {
  await page.goto('/');
  await page.getByLabel('Identificador').fill(username);
  await page.getByLabel('Contraseña').fill(password);
  await page.getByRole('button', { name: 'Iniciar sesión' }).click();
}

export async function loginAsAdmin(page: Page) {
  expect(ADMIN_PASSWORD, 'Definir E2E_PASSWORD con la contraseña del administrador').not.toBe('');
  await login(page);
  await expect(page.getByRole('button', { name: 'Cerrar sesión' })).toBeVisible();
}

export function uniqueCode(prefix: string) {
  return `${prefix}-${Date.now().toString(36).toUpperCase()}${Math.floor(Math.random() * 1000)}`;
}

export async function goTo(page: Page, section: 'Resumen' | 'Puntos' | 'Mediciones' | 'Alertas') {
  await page.getByRole('navigation', { name: 'Navegación principal' }).getByRole('button', { name: section }).click();
}
