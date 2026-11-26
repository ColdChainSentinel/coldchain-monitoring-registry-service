import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { App } from '../../App';
import { setAuthToken } from '../../api/http';
import { json } from '../../test/fixtures';

describe('autenticación y sesión (HU-01)', () => {
  beforeEach(() => {
    setAuthToken(null);
  });

  it('muestra el login, rechaza credenciales inválidas y permite iniciar y cerrar sesión', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async (url: string, options?: RequestInit) => {
        if (url.endsWith('/auth/login')) {
          const parsed = JSON.parse(String(options?.body || '{}'));
          if (parsed.password !== 'Secret123') {
            return json({ detail: 'Credenciales inválidas o cuenta inactiva' }, 401);
          }
          return json({
            token: 'tok-123',
            userId: '11111111-1111-1111-1111-111111111111',
            username: 'admin',
            fullName: 'Administrador',
            role: 'ADMIN',
            allowedPointIds: [],
            message: 'Inicio de sesión exitoso',
          });
        }
        if (url.endsWith('/auth/logout')) return new Response(null, { status: 204 });
        if (url.endsWith('/actuator/health')) return json({ status: 'UP' });
        if (url.includes('/monitoring-points')) return json([]);
        throw new Error('Unexpected route: ' + url);
      }),
    );

    render(<App />);
    expect(screen.getByRole('heading', { name: 'Iniciar sesión' })).toBeInTheDocument();

    await userEvent.type(screen.getByLabelText('Identificador'), 'admin');
    await userEvent.type(screen.getByLabelText('Contraseña'), 'WrongPass');
    await userEvent.click(screen.getByRole('button', { name: 'Iniciar sesión' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Credenciales inválidas o cuenta inactiva');

    await userEvent.clear(screen.getByLabelText('Contraseña'));
    await userEvent.type(screen.getByLabelText('Contraseña'), 'Secret123');
    await userEvent.click(screen.getByRole('button', { name: 'Iniciar sesión' }));

    await waitFor(() => expect(screen.getByText('Cerrar sesión')).toBeInTheDocument());

    await userEvent.click(screen.getByRole('button', { name: 'Cerrar sesión' }));
    expect(await screen.findByRole('heading', { name: 'Iniciar sesión' })).toBeInTheDocument();
  });
});
