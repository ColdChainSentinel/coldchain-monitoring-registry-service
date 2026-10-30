import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { measurement, point } from '../../test/fixtures';
import { MeasurementForm } from './MeasurementForm';

describe('entrada manual de mediciones', () => {
  it('no envía temperaturas vacías ni fechas inválidas', async () => {
    const create = vi.fn();
    render(<MeasurementForm point={point} onCreate={create} />);
    fireEvent.change(screen.getByLabelText('Fecha y hora de medición'), { target: { value: '' } });
    await userEvent.click(screen.getByRole('button', { name: 'Guardar medición' }));
    expect(create).not.toHaveBeenCalled();
    expect(screen.getByText(/Escribe una temperatura numérica/)).toBeInTheDocument();
    expect(screen.getByText(/Selecciona una fecha/)).toBeInTheDocument();
  });
  it('registra negativos, punto real y timestamp UTC, sin reintentar', async () => {
    const create = vi.fn().mockResolvedValue({ ...measurement, temperature: -3.75 });
    render(<MeasurementForm point={point} onCreate={create} />);
    const user = userEvent.setup();
    await user.type(screen.getByLabelText('Temperatura (°C)'), '-3,75');
    fireEvent.change(screen.getByLabelText('Fecha y hora de medición'), {
      target: { value: '2026-10-29T10:00' },
    });
    await user.click(screen.getByRole('button', { name: 'Guardar medición' }));
    expect(create).toHaveBeenCalledExactlyOnceWith({
      monitoringPointId: point.id,
      temperature: -3.75,
      timestamp: new Date('2026-10-29T10:00').toISOString(),
    });
    expect(screen.getByLabelText('Temperatura (°C)')).toHaveValue('');
  });
  it('bloquea doble envío mientras la medición está en curso', async () => {
    let finish!: (value: typeof measurement) => void;
    const create = vi.fn(
      () =>
        new Promise<typeof measurement>((resolve) => {
          finish = resolve;
        }),
    );
    render(<MeasurementForm point={point} onCreate={create} />);
    await userEvent.type(screen.getByLabelText('Temperatura (°C)'), '0');
    await userEvent.click(screen.getByRole('button', { name: 'Guardar medición' }));
    expect(screen.getByRole('button', { name: 'Guardando medición…' })).toBeDisabled();
    expect(create).toHaveBeenCalledTimes(1);
    finish(measurement);
    await waitFor(() => expect(screen.getByRole('button', { name: 'Guardar medición' })).toBeEnabled());
  });
  it('un fallo de red no muestra éxito ni reenvía la medición', async () => {
    const create = vi.fn().mockRejectedValue(new TypeError('Network error'));
    render(<MeasurementForm point={point} onCreate={create} />);
    await userEvent.type(screen.getByLabelText('Temperatura (°C)'), '9');
    await userEvent.click(screen.getByRole('button', { name: 'Guardar medición' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Puede haberse guardado');
    expect(create).toHaveBeenCalledTimes(1);
    expect(screen.getByLabelText('Temperatura (°C)')).toHaveValue('9');
  });
});
