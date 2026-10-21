import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi } from 'vitest';
import { AppProvider } from '../context/AppContext';
import { TransactionsPage } from './TransactionsPage';

vi.mock('../api/api', () => ({ api: {
  expenses: { list: vi.fn().mockResolvedValue([
    { id: 1, description: 'Mercado direto', amountCents: 10000, occurredAt: '2026-10-01T12:00:00', category: { name: 'Casa' }, scope: 'HOUSEHOLD' },
    { id: 2, description: 'Compra na fatura', amountCents: 10000, occurredAt: '2026-10-01T12:00:00', category: { name: 'Casa' }, scope: 'PERSONAL', creditCardInvoice: { id: 5, cardName: 'Cartão' } },
  ]) },
  incomes: { list: vi.fn().mockResolvedValue([]) }, settlements: { list: vi.fn().mockResolvedValue([]) },
  payments: { list: vi.fn().mockResolvedValue([{ id: 1, expenseId: 1, amountCents: 3000, paidAt: '2026-11-01T12:00:00', payerPerson: { name: 'Bia' }, method: 'TRANSFER' }]) },
} }));
it('mostra histórico e restante da despesa direta sem misturar pagamento da fatura', async () => {
  render(<AppProvider><TransactionsPage/></AppProvider>);
  const direct = (await screen.findByText('Mercado direto')).closest('li')!;
  const card = screen.getByText('Compra na fatura').closest('li')!;
  expect(within(card).queryByRole('button', { name: 'Ver pagamentos' })).not.toBeInTheDocument();
  await userEvent.click(within(direct).getByRole('button', { name: 'Ver pagamentos' }));
  const dialog = screen.getByRole('dialog');
  expect(await within(dialog).findByText('Pago: R$ 30,00 · Restante: R$ 70,00')).toBeInTheDocument();
  expect(within(dialog).getByText('Bia')).toBeInTheDocument();
  expect(within(dialog).getByText('Transferência')).toBeInTheDocument();
  expect(within(dialog).getByText('01/11/2026')).toBeInTheDocument();
});
