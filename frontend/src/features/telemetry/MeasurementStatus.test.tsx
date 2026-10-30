import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { alert, measurement } from '../../test/fixtures';
import { AlertAssociation, EvaluationBadge, MeasurementReceipt } from './MeasurementStatus';

describe('evaluación y alertas son hechos distintos', () => {
  it('PENDING nunca se representa como sin alerta', () => {
    const pending = { ...measurement, evaluationStatus: 'PENDING' as const };
    render(
      <>
        <EvaluationBadge status="PENDING" />
        <AlertAssociation measurement={pending} alerts={[]} alertsReady />
      </>,
    );
    expect(screen.getByText('Evaluación pendiente')).toBeInTheDocument();
    expect(screen.queryByText('Sin alerta registrada')).not.toBeInTheDocument();
    expect(screen.getByText('Resultado no confirmado')).toBeInTheDocument();
  });
  it('COMPLETED solo muestra ausencia de alerta tras una consulta de alertas confirmada', () => {
    const { rerender } = render(
      <AlertAssociation measurement={measurement} alerts={null} alertsReady={false} />,
    );
    expect(screen.queryByText('Sin alerta registrada')).not.toBeInTheDocument();
    rerender(<AlertAssociation measurement={measurement} alerts={[]} alertsReady />);
    expect(screen.getByText('Sin alerta registrada')).toBeInTheDocument();
  });
  it('muestra la alerta incluso si Telemetry conserva PENDING', () => {
    render(
      <AlertAssociation
        measurement={{ ...measurement, evaluationStatus: 'PENDING' }}
        alerts={[alert]}
        alertsReady
      />,
    );
    expect(screen.getByText('Alerta registrada')).toBeInTheDocument();
  });
  it.each(['COMPLETED', 'PENDING', 'RULE_NOT_CONFIGURED'] as const)(
    'explica el resultado %s sin fingir NO_ALERT',
    (status) => {
      render(<MeasurementReceipt measurement={{ ...measurement, evaluationStatus: status }} />);
      expect(screen.getByRole('status')).toBeInTheDocument();
      if (status === 'COMPLETED')
        expect(screen.getByText(/no la ausencia de una alerta/)).toBeInTheDocument();
      if (status === 'PENDING') expect(screen.getByText(/no la vuelvas a enviar/)).toBeInTheDocument();
      if (status === 'RULE_NOT_CONFIGURED')
        expect(screen.getByText(/No se aplicaron límites por defecto/)).toBeInTheDocument();
    },
  );
});
