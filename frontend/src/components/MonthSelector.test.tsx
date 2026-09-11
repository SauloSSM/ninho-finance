import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { AppProvider } from '../context/AppContext';
import { MonthSelector } from './MonthSelector';
import { vi } from 'vitest';

it('navega entre os meses e atravessa o ano', async () => {
  vi.setSystemTime(new Date(2026, 0, 10));
  render(<AppProvider><MonthSelector /></AppProvider>);
  expect(screen.getByText('Jan 2026')).toBeInTheDocument();
  await userEvent.click(screen.getByRole('button', { name: 'Mês anterior' }));
  expect(screen.getByText('Dez 2025')).toBeInTheDocument();
  vi.useRealTimers();
});
