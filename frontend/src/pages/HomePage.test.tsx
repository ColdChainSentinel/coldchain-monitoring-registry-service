import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { alert, json, measurement, otherPoint, point } from '../test/fixtures';
import { HomePage } from './HomePage';

function connectedApis() {
  return vi.fn(async (url: string) => {
    if (url.endsWith('/actuator/health')) return json({ status: 'UP' });
    if (url.includes('/monitoring-points')) return json([point, otherPoint]);
    if (url.includes('/measurements')) return json(url.includes(point.id) ? [measurement] : []);
    if (url.includes('/api/alerts')) return json(url.includes(point.id) ? [alert] : []);
    throw new Error('Unexpected route: ' + url);
  });
}

describe('dashboard y navegación', () => {
  it('calcula contadores reales y explicita el alcance por punto', async () => {
    vi.stubGlobal('fetch', connectedApis());
    render(<HomePage />);
    await waitFor(() =>
      expect(
        within(screen.getByRole('article', { name: 'Puntos registrados' })).getByText('2'),
      ).toBeInTheDocument(),
    );
    await waitFor(() =>
      expect(
        within(screen.getByRole('article', { name: 'Mediciones del punto' })).getByText('1'),
      ).toBeInTheDocument(),
    );
    await waitFor(() =>
      expect(
        within(screen.getByRole('article', { name: 'Alertas del punto' })).getByText('1'),
      ).toBeInTheDocument(),
    );
    expect(screen.getByText('Sin estadísticas globales estimadas.')).toBeInTheDocument();
    expect(screen.getAllByText('Temperatura alta').length).toBeGreaterThan(0);
    await userEvent.click(screen.getByRole('button', { name: 'Mediciones' }));
    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('Mediciones térmicas');
    expect(screen.getByLabelText('Temperatura (°C)')).toBeInTheDocument();
  });
  it('cambia el contexto sin mostrar datos del punto anterior', async () => {
    vi.stubGlobal('fetch', connectedApis());
    render(<HomePage />);
    await screen.findByText('Cámara de pruebas', { selector: 'h2' });
    await userEvent.selectOptions(screen.getByLabelText('Punto de monitoreo seleccionado'), otherPoint.id);
    await waitFor(() =>
      expect(
        within(screen.getByRole('article', { name: 'Mediciones del punto' })).getByText('0'),
      ).toBeInTheDocument(),
    );
    expect(screen.queryByText('Temperatura alta')).not.toBeInTheDocument();
  });
  it('las APIs desconectadas producen errores visibles, no indicadores cero ficticios', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('offline')));
    render(<HomePage />);
    expect(await screen.findByRole('alert')).toHaveTextContent('No se pudo comunicar con la API');
    expect(
      within(screen.getByRole('article', { name: 'Puntos registrados' })).queryByText('0'),
    ).not.toBeInTheDocument();
    expect(screen.queryByText('Tu primer punto, el inicio del monitoreo')).not.toBeInTheDocument();
  });
  it('actualiza la lista y selecciona el punto tras un registro real confirmado', async () => {
    const get = connectedApis();
    let saved = false;
    vi.stubGlobal(
      'fetch',
      vi.fn(async (url: string, options?: RequestInit) => {
        if (url.endsWith('/monitoring-points') && options?.method === 'POST') {
          saved = true;
          return json(otherPoint, 201);
        }
        if (url.endsWith('/monitoring-points')) return json(saved ? [point, otherPoint] : [point]);
        return get(url);
      }),
    );
    render(<HomePage />);
    await screen.findByText('Cámara de pruebas', { selector: 'h2' });
    await userEvent.click(screen.getByRole('button', { name: 'Puntos' }));
    await userEvent.type(screen.getByLabelText('Código del punto'), otherPoint.code);
    await userEvent.type(screen.getByLabelText('Nombre'), otherPoint.name);
    await userEvent.selectOptions(screen.getByLabelText('Tipo'), otherPoint.type);
    await userEvent.click(screen.getByRole('button', { name: 'Registrar punto' }));
    expect(await screen.findByText('Punto TR-02 registrado.')).toBeInTheDocument();
    await waitFor(() =>
      expect(screen.getByLabelText('Punto de monitoreo seleccionado')).toHaveValue(otherPoint.id),
    );
    expect(screen.getByRole('heading', { name: otherPoint.name })).toBeInTheDocument();
  });
});
