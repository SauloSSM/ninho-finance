import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react';

type MonthValue = { year: number; month: number; previousMonth: () => void; nextMonth: () => void };
const MonthContext = createContext<MonthValue | null>(null);
const RefreshContext = createContext<{ version: number; refresh: () => void }>({ version: 0, refresh: () => undefined });

export function AppProvider({ children }: { children: ReactNode }) {
  const now = new Date();
  const [date, setDate] = useState({ year: now.getFullYear(), month: now.getMonth() + 1 });
  const [version, setVersion] = useState(0);
  const move = useCallback((amount: number) => setDate((value) => {
    const next = new Date(value.year, value.month - 1 + amount, 1);
    return { year: next.getFullYear(), month: next.getMonth() + 1 };
  }), []);
  const month = useMemo(() => ({ ...date, previousMonth: () => move(-1), nextMonth: () => move(1) }), [date, move]);
  return <MonthContext.Provider value={month}><RefreshContext.Provider value={{ version, refresh: () => setVersion((v) => v + 1) }}>{children}</RefreshContext.Provider></MonthContext.Provider>;
}
export function useMonth() { const value = useContext(MonthContext); if (!value) throw new Error('MonthContext ausente'); return value; }
export const useRefresh = () => useContext(RefreshContext);
