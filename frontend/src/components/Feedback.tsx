import type { ReactNode } from 'react';
import { errorMessage } from '../api/http';
import type { Resource } from '../hooks/useResource';
import { Icon, type IconName } from './Icon';

export function Notice({
  children,
  tone = 'info',
  role = 'status',
}: {
  children: ReactNode;
  tone?: 'info' | 'success' | 'warning' | 'error';
  role?: 'status' | 'alert';
}) {
  return (
    <div className={`notice notice--${tone}`} role={role}>
      <Icon name={tone === 'success' ? 'check' : 'info'} />
      <div>{children}</div>
    </div>
  );
}
export function ResourceFeedback({
  resource,
  onRetry,
}: {
  resource: Resource<unknown>;
  onRetry: () => void;
}) {
  return (
    <>
      {resource.loading && (
        <p className="loading-message" role="status">
          <span className="loading-dot" />
          Cargando datos reales…
        </p>
      )}
      {!!resource.error && (
        <Notice tone="error" role="alert">
          <strong>No se pudo actualizar esta consulta.</strong>
          <p>
            {errorMessage(resource.error)}{' '}
            {resource.data !== null && 'Los datos anteriores pueden estar desactualizados.'}
          </p>
          <button className="text-button" onClick={onRetry}>
            Reintentar consulta
          </button>
        </Notice>
      )}
    </>
  );
}
export function EmptyState({
  title,
  children,
  icon = 'info',
}: {
  title: string;
  children: ReactNode;
  icon?: IconName;
}) {
  return (
    <div className="empty-state">
      <span className="empty-icon">
        <Icon name={icon} />
      </span>
      <h3>{title}</h3>
      <p>{children}</p>
    </div>
  );
}
export function FieldError({ id, message }: { id: string; message?: string }) {
  return message ? (
    <span id={id} className="field-error">
      {message}
    </span>
  ) : null;
}
