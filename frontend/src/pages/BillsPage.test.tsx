import { render, screen, within } from '@testing-library/react';
import { vi } from 'vitest';
import { AppProvider } from '../context/AppContext';
import { BillsPage } from './BillsPage';

vi.mock('../api/api', () => ({ api: {
  bills: { list: vi.fn().mockResolvedValue([
    { id: 1, name: 'Cancelada teste', status: 'CANCELLED', effectiveAmountCents: 7000, paidAmountCents: 2000, outstandingAmountCents: 0, dueDate: '2026-10-01', responsiblePerson: { name: 'Ana' } },
    { id: 2, name: 'Ativa teste', status: 'PARTIALLY_PAID', effectiveAmountCents: 10000, paidAmountCents: 3000, outstandingAmountCents: 7000, dueDate: '2026-10-10', responsiblePerson: { name: 'Ana' } },
  ]) },
  overview: { monthly: vi.fn().mockResolvedValue({ householdBills: { totalCents: 10000, paidCents: 3000, outstandingCents: 7000 } }) },
} }));

it('usa resumo do backend e preserva cancelada no histórico sem dívida', async () => {
  render(<AppProvider><BillsPage /></AppProvider>);
  const cancelled = (await screen.findByText('Cancelada teste')).closest('li')!;
  expect(within(cancelled).getByText('Cancelada')).toBeInTheDocument();
  expect(within(cancelled).getByText('Cancelada · sem valor em aberto')).toBeInTheDocument();
  expect(within(cancelled).queryByText(/falta/)).not.toBeInTheDocument();
  expect(within(cancelled).queryByRole('button', { name: 'Pagar' })).not.toBeInTheDocument();
  for (const [label, amount] of [['Total do mês', 'R$ 100,00'], ['Pago', 'R$ 30,00'], ['Em aberto', 'R$ 70,00']]) {
    expect(within(screen.getByText(label).closest('article')!).getByText(amount)).toBeInTheDocument();
  }
});
