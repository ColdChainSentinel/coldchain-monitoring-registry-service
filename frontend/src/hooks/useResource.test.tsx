import { act, renderHook, waitFor } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { useResource } from './useResource';

describe('consultas sin contaminación entre puntos', () => {
  it('ignora una respuesta tardía del punto anterior', async () => {
    let finish!: (value: string[]) => void;
    const first = vi.fn(
      () =>
        new Promise<string[]>((resolve) => {
          finish = resolve;
        }),
    );
    const second = vi.fn().mockResolvedValue(['punto-b']);
    const { result, rerender } = renderHook(({ id }) => useResource(id, id === 'a' ? first : second), {
      initialProps: { id: 'a' },
    });
    rerender({ id: 'b' });
    await waitFor(() => expect(result.current.data).toEqual(['punto-b']));
    await act(async () => {
      finish(['punto-a']);
    });
    expect(result.current.data).toEqual(['punto-b']);
  });
  it('no confunde un error con una lista vacía y permite reintentar GET', async () => {
    const load = vi.fn().mockRejectedValueOnce(new Error('offline')).mockResolvedValueOnce([]);
    const { result } = renderHook(() => useResource('a', load));
    await waitFor(() => expect(result.current.error).toBeTruthy());
    expect(result.current.data).toBeNull();
    await act(async () => {
      await result.current.reload();
    });
    expect(result.current.data).toEqual([]);
    expect(result.current.error).toBeNull();
  });
});
