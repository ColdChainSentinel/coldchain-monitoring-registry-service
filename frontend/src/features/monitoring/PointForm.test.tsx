import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { HttpError } from '../../api/http';
import { point } from '../../test/fixtures';
import { PointForm } from './PointForm';

async function fillPoint() {
  const user = userEvent.setup();
  await user.type(screen.getByLabelText('Código del punto'), 'cam-01');
  await user.type(screen.getByLabelText('Nombre'), 'Cámara de pruebas');
  await user.selectOptions(screen.getByLabelText('Tipo'), 'ALMACENAMIENTO');
  return user;
}

describe('registro de puntos', () => {
  it('valida obligatorios antes de llamar la API', async () => {
    const create = vi.fn();
    render(<PointForm onCreate={create} />);
    await userEvent.click(screen.getByRole('button', { name: 'Registrar punto' }));
    expect(create).not.toHaveBeenCalled();
    expect(screen.getByText(/Selecciona un tipo de punto/)).toBeInTheDocument();
    expect(screen.getByLabelText('Código del punto')).toHaveAttribute('aria-invalid', 'true');
  });
  it('envía el modelo real y confirma el registro', async () => {
    const create = vi.fn().mockResolvedValue(point);
    render(<PointForm onCreate={create} />);
    const user = await fillPoint();
    await user.click(screen.getByRole('button', { name: 'Registrar punto' }));
    expect(create).toHaveBeenCalledExactlyOnceWith({
      code: 'cam-01',
      name: 'Cámara de pruebas',
      type: 'ALMACENAMIENTO',
    });
    expect(await screen.findByText('Punto CAM-01 registrado.')).toBeInTheDocument();
    expect(screen.getByLabelText('Código del punto')).toHaveValue('');
  });
  it('hace visible un conflicto 409 y conserva los valores', async () => {
    render(<PointForm onCreate={vi.fn().mockRejectedValue(new HttpError(409))} />);
    const user = await fillPoint();
    await user.click(screen.getByRole('button', { name: 'Registrar punto' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('El código ya está registrado');
    expect(screen.getByLabelText('Código del punto')).toHaveValue('cam-01');
  });
  it('asocia errores de validación devueltos por el servidor al campo', async () => {
    render(
      <PointForm
        onCreate={vi
          .fn()
          .mockRejectedValue(new HttpError(400, { fields: { name: 'Nombre inválido de prueba' } }))}
      />,
    );
    const user = await fillPoint();
    await user.click(screen.getByRole('button', { name: 'Registrar punto' }));
    expect(await screen.findByText('Nombre inválido de prueba')).toBeInTheDocument();
    expect(screen.getByLabelText('Nombre')).toHaveAccessibleDescription('Nombre inválido de prueba');
  });
});
