import { useCallback, useEffect, useRef, useState } from 'react';
import { useRefresh } from '../context/AppContext';

export function useLoad<T>(loader: () => Promise<T>, dependencies: unknown[] = []) {
  const { version } = useRefresh();
  const [data, setData] = useState<T>();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const requestId = useRef(0);
  const reload = useCallback(async () => {
    const current = ++requestId.current;
    setLoading(true); setError('');
    try {
      const result = await loader();
      if (current === requestId.current) setData(result);
    } catch (reason) {
      if (current === requestId.current) setError(reason instanceof Error ? reason.message : 'Não foi possível carregar os dados.');
    } finally {
      if (current === requestId.current) setLoading(false);
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, dependencies);
  useEffect(() => {
    void reload();
    return () => { ++requestId.current; };
  }, [reload, version]);
  return { data, loading, error, reload };
}
