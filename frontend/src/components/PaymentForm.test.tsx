import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi } from 'vitest';
import { AppProvider } from '../context/AppContext';
import { PaymentForm } from './forms';

vi.mock('../api/api', () => ({ api: {
  people: { list: vi.fn().mockResolvedValue([]) }, categories: { list: vi.fn().mockResolvedValue([]) },
  accounts: { list: vi.fn().mockResolvedValue([]) }, cards: { list: vi.fn().mockResolvedValue([]) },
  invoices: { list: vi.fn().mockResolvedValue([{ id: 1, creditCard: { name: 'Fatura teste' } }]) },
  bills: { list: vi.fn().mockResolvedValue([]) },
  expenses: { list: vi.fn().mockResolvedValue([
    { id: 1, description: 'Fora do cartão', creditCardInvoice: null },
    { id: 2, description: 'Compra no cartão', creditCardInvoice: { id: 1 } },
  ]) },
} }));

it('oferece somente despesas fora de fatura para pagamento direto', async () => {
  render(<AppProvider><PaymentForm defaultTarget="expense" onDone={() => undefined} /></AppProvider>);
  const item = screen.getByLabelText('Item');
  expect(await within(item).findByRole('option', { name: 'Fora do cartão' })).toBeInTheDocument();
  expect(within(item).queryByRole('option', { name: 'Compra no cartão' })).not.toBeInTheDocument();
  await userEvent.selectOptions(screen.getByLabelText('O que foi pago?'), 'invoice');
  expect(await screen.findByRole('option', { name: 'Fatura teste' })).toBeInTheDocument();
});
