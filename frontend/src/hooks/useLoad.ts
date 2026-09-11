import { useCallback, useEffect, useState } from 'react';
import { useRefresh } from '../context/AppContext';

export function useLoad<T>(loader: () => Promise<T>, dependencies: unknown[] = []) {
  const { version } = useRefresh();
  const [data, setData] = useState<T>();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const reload = useCallback(async () => {
    setLoading(true); setError('');
    try { setData(await loader()); } catch (reason) { setError(reason instanceof Error ? reason.message : 'Não foi possível carregar os dados.'); }
    finally { setLoading(false); }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, dependencies);
  useEffect(() => { void reload(); }, [reload, version]);
  return { data, loading, error, reload };
}
