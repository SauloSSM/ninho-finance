import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, vi } from 'vitest';
import { api } from '../api/api';
import type { CreditCard, Invoice } from '../api/types';
import { AppProvider } from '../context/AppContext';
import { CardsPage } from './CardsPage';

vi.mock('../api/api', () => ({ api: { cards: { list: vi.fn() }, invoices: { list: vi.fn(), close: vi.fn() }, expenses: { list: vi.fn() }, payments: { list: vi.fn() } } }));
const card = { id: 1, name: 'Cartão da casa', institution: 'Banco', active: true, creditLimitCents: 123456 } as CreditCard;
const invoice = { id: 10, creditCard: { id: 1, name: card.name }, referenceMonth: 10, referenceYear: 2026, dueDate: '2026-10-15', status: 'OPEN', calculatedTotalCents: 10000, paidAmountCents: 2000, outstandingAmountCents: 8000 } as Invoice;
beforeEach(() => {
  vi.clearAllMocks();
  vi.mocked(api.cards.list).mockResolvedValue([card]);
  vi.mocked(api.invoices.list).mockResolvedValue([invoice]);
  vi.mocked(api.expenses.list).mockResolvedValue([]);
  vi.mocked(api.payments.list).mockResolvedValue([]);
});
it('carrega todas as compras pelo vínculo da fatura, sem filtro de data', async () => {
  vi.mocked(api.expenses.list).mockResolvedValue([{ id: 1, description: 'Compra do mês anterior', category: { id: 1, name: 'Mercado' }, amountCents: 10000, occurredAt: '2026-09-30T12:00:00', scope: 'HOUSEHOLD', allocations: [], createdAt: '' }]);
  render(<AppProvider><CardsPage/></AppProvider>);
  expect(await screen.findByText('Compra do mês anterior')).toBeInTheDocument();
  expect(api.expenses.list).toHaveBeenCalledWith(undefined, undefined, 10);
  expect(screen.getByText('30/09/2026')).toBeInTheDocument();
});
it('mantém fatura e pagamentos acessíveis no cartão inativo sem oferecer novas operações', async () => {
  vi.mocked(api.cards.list).mockResolvedValue([{ ...card, active: false }]);
  render(<AppProvider><CardsPage/></AppProvider>);
  expect(await screen.findByText('Inativo')).toBeInTheDocument();
  expect(await screen.findByText('Pagamentos registrados')).toBeInTheDocument();
  expect(api.cards.list).toHaveBeenCalledWith();
  expect(screen.getByRole('button', { name: 'Registrar pagamento' })).toBeInTheDocument();
  expect(screen.queryByRole('button', { name: 'Alterar limite' })).not.toBeInTheDocument();
  expect(screen.queryByRole('button', { name: '+ Criar fatura' })).not.toBeInTheDocument();
});
it('não oferece criar fatura em cartão inativo sem fatura no mês', async () => {
  vi.mocked(api.cards.list).mockResolvedValue([{ ...card, active: false }]);
  vi.mocked(api.invoices.list).mockResolvedValue([]);
  render(<AppProvider><CardsPage/></AppProvider>);
  expect(await screen.findByText(/Cartão inativo. Selecione outro mês/)).toBeInTheDocument();
  expect(screen.queryByRole('button', { name: '+ Criar fatura' })).not.toBeInTheDocument();
});
it('preenche o ajuste com o limite atual', async () => {
  render(<AppProvider><CardsPage/></AppProvider>);
  await userEvent.click(await screen.findByRole('button', { name: 'Alterar limite' }));
  expect(screen.getByLabelText('Novo limite')).toHaveValue('R$ 1.234,56');
});
it('exibe erro de compras com nova tentativa em vez de lista vazia', async () => {
  vi.mocked(api.expenses.list).mockRejectedValueOnce(new Error('Falha nas compras'));
  render(<AppProvider><CardsPage/></AppProvider>);
  expect(await screen.findByText('Falha nas compras')).toBeInTheDocument();
  await userEvent.click(screen.getByRole('button', { name: 'Tentar novamente' }));
  expect(await screen.findByText('Nenhuma compra nesta fatura')).toBeInTheDocument();
});
it('só fecha fatura após confirmação', async () => {
  const confirm = vi.spyOn(window, 'confirm').mockReturnValue(false);
  vi.mocked(api.invoices.close).mockResolvedValue(invoice);
  render(<AppProvider><CardsPage/></AppProvider>);
  await userEvent.click(await screen.findByRole('button', { name: 'Fechar fatura' }));
  expect(api.invoices.close).not.toHaveBeenCalled();
  confirm.mockReturnValue(true);
  await userEvent.click(screen.getByRole('button', { name: 'Fechar fatura' }));
  await waitFor(() => expect(api.invoices.close).toHaveBeenCalledWith(10));
  confirm.mockRestore();
});
