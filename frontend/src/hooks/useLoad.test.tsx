import { act, renderHook, waitFor } from '@testing-library/react';
import { vi } from 'vitest';
import { AppProvider } from '../context/AppContext';
import { useLoad } from './useLoad';

function deferred<T>() {
  let resolve!: (value: T) => void;
  let reject!: (error: Error) => void;
  const promise = new Promise<T>((yes, no) => { resolve = yes; reject = no; });
  return { promise, resolve, reject };
}

it('preserva B quando A termina depois de uma mudança de mês', async () => {
  const a = deferred<string>(); const b = deferred<string>();
  const load = vi.fn((month: number) => month === 1 ? a.promise : b.promise);
  const { result, rerender } = renderHook(({ month }) => useLoad(() => load(month), [month]), {
    initialProps: { month: 1 }, wrapper: AppProvider,
  });
  rerender({ month: 2 });
  await act(async () => { b.resolve('B'); });
  await waitFor(() => expect(result.current.data).toBe('B'));
  await act(async () => { a.resolve('A'); });
  expect(result.current.data).toBe('B');
  expect(result.current.loading).toBe(false);
  expect(result.current.error).toBe('');
});

it('ignora erro e finally de uma requisição antiga durante a atual', async () => {
  const a = deferred<string>(); const b = deferred<string>();
  const { result, rerender } = renderHook(({ month }) => useLoad(() => month === 1 ? a.promise : b.promise, [month]), {
    initialProps: { month: 1 }, wrapper: AppProvider,
  });
  rerender({ month: 2 });
  await act(async () => { a.reject(new Error('Erro antigo')); });
  expect(result.current.loading).toBe(true);
  expect(result.current.error).toBe('');
  await act(async () => { b.resolve('B'); });
  expect(result.current.data).toBe('B');
  expect(result.current.loading).toBe(false);
});

it('protege também recarregamentos manuais concorrentes', async () => {
  const a = deferred<string>(); const b = deferred<string>();
  const loader = vi.fn().mockReturnValueOnce(a.promise).mockReturnValueOnce(b.promise);
  const { result } = renderHook(() => useLoad(loader), { wrapper: AppProvider });
  act(() => { void result.current.reload(); });
  await act(async () => { b.resolve('B'); });
  await act(async () => { a.resolve('A'); });
  expect(result.current.data).toBe('B');
});
