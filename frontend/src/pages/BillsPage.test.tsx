import { render, screen, within } from '@testing-library/react';
import { vi } from 'vitest';
import userEvent from '@testing-library/user-event';
import { api } from '../api/api';
import { AppProvider } from '../context/AppContext';
import { BillsPage } from './BillsPage';

vi.mock('../api/api', () => ({ api: {
  bills: { cancel: vi.fn(), list: vi.fn().mockResolvedValue([
    { id: 1, name: 'Cancelada teste', status: 'CANCELLED', effectiveAmountCents: 7000, paidAmountCents: 2000, outstandingAmountCents: 0, dueDate: '2026-10-01', responsiblePerson: { name: 'Ana' } },
    { id: 2, name: 'Ativa teste', status: 'PARTIALLY_PAID', effectiveAmountCents: 10000, paidAmountCents: 3000, outstandingAmountCents: 7000, dueDate: '2026-10-10', responsiblePerson: { name: 'Ana' } },
  ]) },
  overview: { monthly: vi.fn().mockResolvedValue({ householdBills: { totalCents: 10000, paidCents: 3000, outstandingCents: 7000 } }) },
  payments: { list: vi.fn().mockResolvedValue([{ id: 1, householdBillId: 1, amountCents: 2000, paidAt: '2026-09-01T12:00:00', payerPerson: { id: 1, name: 'Ana' }, method: 'PIX' }]) },
} }));

it('usa resumo do backend e preserva cancelada no histórico sem dívida', async () => {
  render(<AppProvider><BillsPage /></AppProvider>);
  const cancelled = (await screen.findByText('Cancelada teste')).closest('li')!;
  expect(within(cancelled).getByText('Cancelada')).toBeInTheDocument();
  expect(within(cancelled).getByText('Cancelada · sem valor em aberto')).toBeInTheDocument();
  expect(within(cancelled).queryByText(/falta/)).not.toBeInTheDocument();
  expect(within(cancelled).queryByRole('button', { name: 'Pagar' })).not.toBeInTheDocument();
  expect(within(cancelled).queryByRole('button', { name: 'Ajustar valor' })).not.toBeInTheDocument();
  for (const [label, amount] of [['Total do mês', 'R$ 100,00'], ['Pago', 'R$ 30,00'], ['Em aberto', 'R$ 70,00']]) {
    expect(within(screen.getByText(label).closest('article')!).getByText(amount)).toBeInTheDocument();
  }
});
it('preenche ajuste com valor atual e exclui canceladas do filtro pendente', async () => {
  render(<AppProvider><BillsPage /></AppProvider>);
  await userEvent.click(await screen.findByRole('button', { name: 'Ajustar valor' }));
  expect(screen.getByLabelText('Valor real')).toHaveValue('R$ 100,00');
  await userEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Cancelar' }));
  await userEvent.click(screen.getByRole('tab', { name: 'Pendentes' }));
  expect(screen.queryByText('Cancelada teste')).not.toBeInTheDocument();
  expect(screen.getByText('Ativa teste')).toBeInTheDocument();
});
it('pede confirmação antes de cancelar conta', async () => {
  const confirm = vi.spyOn(window, 'confirm').mockReturnValue(false);
  render(<AppProvider><BillsPage /></AppProvider>);
  await userEvent.click(await screen.findByRole('button', { name: 'Cancelar' }));
  expect(confirm).toHaveBeenCalled();
  expect(api.bills.cancel).not.toHaveBeenCalled();
  confirm.mockRestore();
});
it('permite entender o valor pago mesmo após cancelamento', async () => {
  render(<AppProvider><BillsPage /></AppProvider>);
  const row = (await screen.findByText('Cancelada teste')).closest('li')!;
  await userEvent.click(within(row).getByRole('button', { name: 'Ver pagamentos' }));
  const dialog = screen.getByRole('dialog');
  expect(await within(dialog).findByText('Pago: R$ 20,00')).toBeInTheDocument();
  expect(within(dialog).getByText('01/09/2026')).toBeInTheDocument();
  expect(within(dialog).getByText('Ana')).toBeInTheDocument();
});
