import { createContext, useCallback, useContext, useState, type ReactNode } from 'react';
type Toast = { id: number; text: string; kind: 'success' | 'error' };
const ToastContext = createContext<(text: string, kind?: Toast['kind']) => void>(() => undefined);
export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([]);
  const notify = useCallback((text: string, kind: Toast['kind'] = 'success') => {
    const id = Date.now() + Math.random(); setToasts((all) => [...all, { id, text, kind }]);
    window.setTimeout(() => setToasts((all) => all.filter((toast) => toast.id !== id)), 3500);
  }, []);
  return <ToastContext.Provider value={notify}>{children}<div className="toast-region" role="status" aria-live="polite">{toasts.map((toast) => <div className={`toast ${toast.kind}`} key={toast.id}>{toast.text}</div>)}</div></ToastContext.Provider>;
}
export const useToast = () => useContext(ToastContext);
