import { useCallback, useEffect, useRef, useState } from 'react';

export interface Resource<T> {
  data: T | null;
  loading: boolean;
  error: unknown;
  updatedAt: Date | null;
}
interface State<T> extends Resource<T> {
  key: string | null;
}

export function useResource<T>(key: string | null, load: (signal: AbortSignal) => Promise<T>, revision = 0) {
  const [state, setState] = useState<State<T>>({
    key: null,
    data: null,
    loading: false,
    error: null,
    updatedAt: null,
  });
  const request = useRef<{ sequence: number; controller?: AbortController }>({ sequence: 0 });
  const reload = useCallback(async () => {
    request.current.controller?.abort();
    const sequence = ++request.current.sequence;
    if (!key) return;
    const controller = new AbortController();
    request.current.controller = controller;
    setState((previous) => ({
      key,
      data: previous.key === key ? previous.data : null,
      updatedAt: previous.key === key ? previous.updatedAt : null,
      loading: true,
      error: null,
    }));
    try {
      const data = await load(controller.signal);
      if (sequence === request.current.sequence && !controller.signal.aborted) {
        setState({ key, data, loading: false, error: null, updatedAt: new Date() });
      }
    } catch (error) {
      if (sequence === request.current.sequence && !controller.signal.aborted) {
        setState((previous) => ({ ...previous, loading: false, error }));
      }
    }
  }, [key, load]);

  useEffect(() => {
    void reload();
    return () => {
      request.current.sequence++;
      request.current.controller?.abort();
    };
  }, [reload, revision]);

  const update = useCallback(
    (change: (current: T) => T) => {
      setState((previous) =>
        previous.key === key && previous.data !== null
          ? { ...previous, data: change(previous.data) }
          : previous,
      );
    },
    [key],
  );
  const current =
    key && state.key === key ? state : { data: null, loading: !!key, error: null, updatedAt: null };
  return { ...current, reload, update };
}
