import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, vi } from 'vitest';
import { AppProvider } from '../context/AppContext';
import { api } from '../api/api';
import { SettingsPage } from './SettingsPage';

vi.mock('../api/api', () => ({ api: {
  people: { list: vi.fn().mockResolvedValue([{ id: 1, name: 'Ana', type: 'USER', active: true }, { id: 2, name: 'Bia', type: 'EXTERNAL', active: false }]), status: vi.fn().mockResolvedValue({}) },
  categories: { list: vi.fn().mockResolvedValue([]) }, accounts: { list: vi.fn().mockResolvedValue([]) },
  cards: { list: vi.fn().mockResolvedValue([{ id: 1, name: 'Cartão', institution: 'Banco', creditLimitCents: 45678, active: true }]) },
} }));
beforeEach(() => vi.clearAllMocks());
it('traduz cadastro ativo e inativo sem chamar inativo de cancelado', async () => {
  render(<AppProvider><SettingsPage/></AppProvider>);
  expect(await screen.findByText('Ativo')).toBeInTheDocument();
  expect(screen.getByText('Inativo')).toBeInTheDocument();
  expect(screen.queryByText('Cancelada')).not.toBeInTheDocument();
  expect(screen.getByText('Contato externo')).toBeInTheDocument();
});
it('pede confirmação para desativar e permite ativar diretamente', async () => {
  const confirm = vi.spyOn(window, 'confirm').mockReturnValue(false);
  render(<AppProvider><SettingsPage/></AppProvider>);
  await userEvent.click(await screen.findByRole('button', { name: 'Desativar' }));
  expect(api.people.status).not.toHaveBeenCalled();
  confirm.mockReturnValue(true);
  await userEvent.click(screen.getByRole('button', { name: 'Desativar' }));
  await waitFor(() => expect(api.people.status).toHaveBeenCalledWith(1, false));
  confirm.mockClear();
  await userEvent.click(screen.getByRole('button', { name: 'Ativar' }));
  expect(api.people.status).toHaveBeenCalledWith(2, true);
  expect(confirm).not.toHaveBeenCalled();
  confirm.mockRestore();
});
it('preenche limite atual também nas configurações', async () => {
  render(<AppProvider><SettingsPage/></AppProvider>);
  await screen.findByText('Ana');
  await userEvent.click(screen.getByRole('button', { name: 'Cartões' }));
  await userEvent.click(screen.getByRole('button', { name: 'Alterar limite' }));
  expect(screen.getByLabelText('Novo limite')).toHaveValue('R$ 456,78');
});
