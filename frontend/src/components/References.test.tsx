import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, vi } from 'vitest';
import { api } from '../api/api';
import { AppProvider } from '../context/AppContext';
import { CardForm, ExpenseForm, IncomeForm, InvoiceForm, SetupForm } from './forms';

vi.mock('../api/api', () => ({ api: Object.fromEntries(['people', 'categories', 'accounts', 'cards', 'invoices'].map(key => [key, { list: vi.fn(), create: vi.fn() }])) }));
beforeEach(() => {
  vi.clearAllMocks();
  for (const resource of [api.people, api.categories, api.accounts, api.cards, api.invoices]) vi.mocked(resource.list).mockResolvedValue([]);
});
it.each([
  ['people', 'pessoas', CardForm, 'Salvar cartão'],
  ['categories', 'categorias', ExpenseForm, 'Salvar despesa'],
  ['accounts', 'contas bancárias', IncomeForm, 'Salvar renda'],
  ['cards', 'cartões', InvoiceForm, 'Criar fatura'],
  ['invoices', 'faturas', ExpenseForm, 'Salvar despesa'],
] as const)('bloqueia formulário e permite tentar novamente quando %s falha', async (key, label, Form, submitLabel) => {
  vi.mocked(api[key].list).mockRejectedValueOnce(new Error('Network failure'));
  render(<AppProvider><Form onDone={() => undefined}/></AppProvider>);
  expect(await screen.findByText(`Não foi possível carregar ${label}. Tente novamente antes de salvar.`)).toBeInTheDocument();
  expect(screen.queryByRole('button', { name: submitLabel })).not.toBeInTheDocument();
  expect(screen.queryByRole('combobox')).not.toBeInTheDocument();
  await userEvent.click(screen.getByRole('button', { name: 'Tentar novamente' }));
  expect(await screen.findByRole('button', { name: submitLabel })).toBeInTheDocument();
});
it('permite cadastrar pessoa mesmo quando cartões estão indisponíveis', async () => {
  render(<AppProvider><SetupForm kind="person" onDone={() => undefined}/></AppProvider>);
  expect(await screen.findByLabelText('Nome')).toBeInTheDocument();
  expect(api.cards.list).not.toHaveBeenCalled();
});
