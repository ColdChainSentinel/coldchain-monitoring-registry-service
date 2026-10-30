import type { Decimal } from './decimal';

const dateFormatter = new Intl.DateTimeFormat('es-PE', { dateStyle: 'medium', timeStyle: 'medium' });
export const formatDate = (value: string | Date) => dateFormatter.format(new Date(value));
export const formatTemperature = (value: Decimal) =>
  `${typeof value === 'number' ? String(value) : value.raw} °C`;
export const pointTypeLabel = (value: string) =>
  value === 'ALMACENAMIENTO' ? 'Almacenamiento' : 'Transporte';
export const localNow = () => {
  const now = new Date();
  return new Date(now.getTime() - now.getTimezoneOffset() * 60_000).toISOString().slice(0, 19);
};
