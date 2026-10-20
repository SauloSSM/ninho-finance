import { afterEach, expect, it, vi } from 'vitest';
import { todayIso } from './date';

afterEach(() => { vi.restoreAllMocks(); vi.useRealTimers(); });

it('usa o dia local quando UTC já virou para o dia seguinte', () => {
  vi.useFakeTimers();
  vi.setSystemTime(new Date('2026-10-21T01:30:00Z'));
  // Simula 20/10 às 22:30 em UTC-3, independentemente do fuso da máquina de CI.
  vi.spyOn(Date.prototype, 'getFullYear').mockReturnValue(2026);
  vi.spyOn(Date.prototype, 'getMonth').mockReturnValue(9);
  vi.spyOn(Date.prototype, 'getDate').mockReturnValue(20);
  expect(new Date().toISOString().slice(0, 10)).toBe('2026-10-21');
  expect(todayIso()).toBe('2026-10-20');
});

it('formata mês e dia locais com dois dígitos', () => {
  vi.useFakeTimers();
  vi.setSystemTime(new Date(2026, 0, 2, 0, 15));
  expect(todayIso()).toBe('2026-01-02');
});
