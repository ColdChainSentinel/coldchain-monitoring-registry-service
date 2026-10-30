import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { alert } from '../../test/fixtures';
import { AlertTable } from './AlertTable';

describe('alertas de la API', () => {
  it('muestra tipo, estado, temperatura, límites e identificador real', () => {
    render(
      <AlertTable alerts={[alert, { ...alert, id: 'low-test', type: 'LOW_TEMPERATURE', temperature: 1 }]} />,
    );
    expect(screen.getByText('Temperatura alta')).toBeInTheDocument();
    expect(screen.getByText('Temperatura baja')).toBeInTheDocument();
    expect(screen.getAllByText('Abierta (OPEN)')).toHaveLength(2);
    expect(screen.getByText('9 °C')).toBeInTheDocument();
    expect(screen.getAllByText(alert.measurementId)).toHaveLength(2);
    expect(screen.getAllByText(/2 °C/)).toHaveLength(2);
  });
  it('no inventa type cuando el campo opcional falta', () => {
    render(<AlertTable alerts={[{ ...alert, type: undefined }]} />);
    expect(screen.getByText('Tipo no informado')).toBeInTheDocument();
  });
  it('una consulta vacía no se presenta como todas las mediciones normales', () => {
    render(<AlertTable alerts={[]} />);
    expect(screen.getByText('Sin alertas registradas para este punto')).toBeInTheDocument();
    expect(screen.getByText(/no confirma que todas las mediciones/)).toBeInTheDocument();
  });
});
