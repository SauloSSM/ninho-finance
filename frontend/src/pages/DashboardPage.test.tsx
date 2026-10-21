import { render, screen, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { vi } from 'vitest';
import { AppProvider } from '../context/AppContext';
import { DashboardPage } from './DashboardPage';
import { api } from '../api/api';
import type { OverviewBill } from '../api/types';
vi.mock('../api/api', () => ({ api:{ overview:{ monthly:vi.fn().mockRejectedValue(new Error('Backend indisponível')), bills:vi.fn(), invoices:vi.fn() } } }));
it('exibe estado de erro quando o backend não responde', async () => {
  render(<MemoryRouter><AppProvider><DashboardPage /></AppProvider></MemoryRouter>);
  expect(await screen.findByText('Não foi possível carregar')).toBeInTheDocument();
  expect(screen.getByText('Backend indisponível')).toBeInTheDocument();
});

function overview(hasActivity: boolean) {
  return {
    year: 2026, month: 10, hasActivity, outstandingCents: 12345,
    income: { expectedCents: 0, receivedCents: 0 },
    householdBills: { totalCents: 0, paidCents: 0, outstandingCents: 0, overdueCents: 0 },
    creditCards: { totalCents: 0, paidCents: 0, outstandingCents: 0 },
    expenses: { personalCents: 0, householdCents: 0, totalCents: 0, paidCents: 0, outstandingCents: 0 },
  };
}

it.each(['renda recebida', 'despesa', 'pagamento', 'acerto'])('não oculta mês com apenas %s', async (activity) => {
  const data = overview(true);
  if (activity === 'renda recebida') data.income.receivedCents = 15000;
  if (activity === 'despesa') data.expenses.totalCents = 10000;
  vi.mocked(api.overview.monthly).mockResolvedValueOnce(data);
  vi.mocked(api.overview.bills).mockResolvedValueOnce([]);
  vi.mocked(api.overview.invoices).mockResolvedValueOnce([]);
  render(<MemoryRouter><AppProvider><DashboardPage /></AppProvider></MemoryRouter>);
  expect(await screen.findByText('Entradas recebidas')).toBeInTheDocument();
  expect(screen.queryByText('Seu Ninho está começando.')).not.toBeInTheDocument();
  // O total vem do backend, não da soma dos componentes no frontend.
  expect(screen.getByText('R$ 123,45')).toBeInTheDocument();
  if (activity === 'renda recebida') expect(screen.getByText('R$ 150,00')).toBeInTheDocument();
});

it('mostra vazio quando o backend informa mês sem atividade', async () => {
  vi.mocked(api.overview.monthly).mockResolvedValueOnce({ ...overview(false), outstandingCents: 0 });
  vi.mocked(api.overview.bills).mockResolvedValueOnce([]);
  vi.mocked(api.overview.invoices).mockResolvedValueOnce([]);
  render(<MemoryRouter><AppProvider><DashboardPage /></AppProvider></MemoryRouter>);
  expect(await screen.findByText('Seu Ninho está começando.')).toBeInTheDocument();
});
it('prioriza atrasadas e ordena próximas contas sem pagas ou canceladas', async () => {
  vi.mocked(api.overview.monthly).mockResolvedValueOnce(overview(true));
  vi.mocked(api.overview.invoices).mockResolvedValueOnce([]);
  vi.mocked(api.overview.bills).mockResolvedValueOnce([
    { id: 1, name: 'Paga teste', status: 'PAID', dueDate: '2026-10-01', overdue: true },
    { id: 2, name: 'Cancelada teste', status: 'CANCELLED', dueDate: '2026-10-01', overdue: true },
    { id: 3, name: 'Pendente distante', status: 'PENDING', dueDate: '2026-10-30', overdue: false },
    { id: 4, name: 'Parcial atrasada', status: 'PARTIALLY_PAID', dueDate: '2026-10-10', overdue: true },
    { id: 5, name: 'Pendente próxima', status: 'PENDING', dueDate: '2026-10-25', overdue: false },
  ] as OverviewBill[]);
  render(<MemoryRouter><AppProvider><DashboardPage /></AppProvider></MemoryRouter>);
  const section = (await screen.findByText('Próximas contas')).closest('section')!;
  const rows = within(section).getAllByRole('listitem');
  expect(rows.map(row => row.querySelector('.row-primary')?.textContent)).toEqual(['Parcial atrasada', 'Pendente próxima', 'Pendente distante']);
  expect(screen.queryByText('Paga teste')).not.toBeInTheDocument();
  expect(screen.queryByText('Cancelada teste')).not.toBeInTheDocument();
  expect(screen.getByText('1 conta está atrasada')).toBeInTheDocument();
});
