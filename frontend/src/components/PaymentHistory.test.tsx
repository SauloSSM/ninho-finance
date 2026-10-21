import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi } from 'vitest';
import { api } from '../api/api';
import { AppProvider } from '../context/AppContext';
import { PaymentHistory } from './PaymentHistory';

vi.mock('../api/api', () => ({ api: { payments: { list: vi.fn() } } }));
it.each(['expenseId', 'householdBillId', 'creditCardInvoiceId'] as const)('consulta histórico completo por %s sem filtro de mês', async target => {
  vi.mocked(api.payments.list).mockResolvedValue([
    { id: 1, [target]: 10, payerPerson: { id: 1, name: 'Ana' }, method: 'PIX', bankAccount: { id: 1, name: 'Conta da casa' }, amountCents: 2000, paidAt: '2025-12-01T12:00:00' },
    { id: 2, [target]: 10, payerPerson: { id: 2, name: 'Bia' }, method: 'CASH', amountCents: 3000, paidAt: '2026-01-02T12:00:00' },
    { id: 3, [target]: 99, payerPerson: { id: 3, name: 'Outro pagamento' }, method: 'OTHER', amountCents: 9999, paidAt: '2026-01-03T12:00:00' },
  ]);
  render(<AppProvider><PaymentHistory target={target} id={10} amountCents={10000}/></AppProvider>);
  expect(await screen.findByText('Pago: R$ 50,00 · Restante: R$ 50,00')).toBeInTheDocument();
  const rows = screen.getAllByRole('listitem');
  expect(within(rows[0]).getByText('Bia')).toBeInTheDocument();
  expect(within(rows[1]).getByText('Ana')).toBeInTheDocument();
  expect(screen.getByText('Pix · Conta da casa')).toBeInTheDocument();
  expect(screen.getByText('Dinheiro')).toBeInTheDocument();
  expect(screen.getByText('01/12/2025')).toBeInTheDocument();
  expect(screen.queryByText('Outro pagamento')).not.toBeInTheDocument();
  expect(api.payments.list).toHaveBeenCalledWith();
});
it('oferece nova tentativa quando a consulta falha', async () => {
  vi.mocked(api.payments.list).mockRejectedValueOnce(new Error('Sem conexão')).mockResolvedValueOnce([]);
  render(<AppProvider><PaymentHistory target="expenseId" id={1}/></AppProvider>);
  await userEvent.click(await screen.findByRole('button', { name: 'Tentar novamente' }));
  expect(await screen.findByText('Nenhum pagamento registrado')).toBeInTheDocument();
});
