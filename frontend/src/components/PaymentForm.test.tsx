import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi } from 'vitest';
import { AppProvider } from '../context/AppContext';
import { PaymentForm } from './forms';
import { api } from '../api/api';

vi.mock('../api/api', () => ({ api: {
  people: { list: vi.fn().mockResolvedValue([]) }, categories: { list: vi.fn().mockResolvedValue([]) },
  accounts: { list: vi.fn().mockResolvedValue([]) }, cards: { list: vi.fn().mockResolvedValue([]) },
  invoices: { list: vi.fn().mockResolvedValue([{ id: 1, creditCard: { name: 'Fatura teste' }, outstandingAmountCents: 10000 }]) },
  payments: { list: vi.fn().mockResolvedValue([{ expenseId: 3, amountCents: 1000 }]) },
  bills: { list: vi.fn().mockResolvedValue([]) },
  expenses: { list: vi.fn().mockResolvedValue([
    { id: 1, description: 'Fora do cartão', creditCardInvoice: null, amountCents: 1000 },
    { id: 2, description: 'Compra no cartão', creditCardInvoice: { id: 1 } },
    { id: 3, description: 'Despesa já paga', creditCardInvoice: null, amountCents: 1000 },
  ]) },
} }));

it('oferece somente despesas fora de fatura para pagamento direto', async () => {
  render(<AppProvider><PaymentForm onDone={() => undefined} /></AppProvider>);
  await userEvent.selectOptions(await screen.findByLabelText('O que foi pago?'), 'expense');
  const item = await screen.findByLabelText('Item');
  expect(await within(item).findByRole('option', { name: 'Fora do cartão' })).toBeInTheDocument();
  expect(within(item).queryByRole('option', { name: 'Compra no cartão' })).not.toBeInTheDocument();
  expect(within(item).queryByRole('option', { name: 'Despesa já paga' })).not.toBeInTheDocument();
  await userEvent.selectOptions(screen.getByLabelText('O que foi pago?'), 'invoice');
  expect(await screen.findByRole('option', { name: 'Fatura teste' })).toBeInTheDocument();
});
it('preserva seleção inicial após referências carregarem e mantém o contexto do pagamento', async () => {
  render(<AppProvider><PaymentForm defaultTarget="invoice" defaultId={1} onDone={() => undefined} /></AppProvider>);
  expect(await screen.findByLabelText('Item')).toHaveValue('1');
  expect(screen.getByLabelText('O que foi pago?')).toBeDisabled();
});
it('não oferece fatura sem valor restante para pagamento', async () => {
  vi.mocked(api.invoices.list).mockResolvedValueOnce([
    { id: 2, creditCard: { id: 1, name: 'Já quitada' }, outstandingAmountCents: 0 } as Awaited<ReturnType<typeof api.invoices.list>>[number],
  ]);
  render(<AppProvider><PaymentForm defaultTarget="invoice" onDone={() => undefined} /></AppProvider>);
  const item = await screen.findByLabelText('Item');
  expect(within(item).queryByRole('option', { name: 'Já quitada' })).not.toBeInTheDocument();
  expect(within(item).getByRole('option', { name: 'Nenhum item com valor em aberto' })).toBeInTheDocument();
});
