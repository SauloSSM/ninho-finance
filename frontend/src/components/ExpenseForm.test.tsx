import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi } from 'vitest';
import { ExpenseForm } from './forms';
import { AppProvider } from '../context/AppContext';
import { ToastProvider } from '../context/ToastContext';

vi.mock('../api/api', () => ({ api: { people:{list:vi.fn().mockResolvedValue([{id:1,name:'Pessoa A'},{id:2,name:'Pessoa B'}])}, categories:{list:vi.fn().mockResolvedValue([{id:1,name:'Mercado',type:'EXPENSE'}])}, accounts:{list:vi.fn().mockResolvedValue([])}, cards:{list:vi.fn().mockResolvedValue([])}, invoices:{list:vi.fn().mockResolvedValue([])}, bills:{list:vi.fn().mockResolvedValue([])}, expenses:{list:vi.fn().mockResolvedValue([]),create:vi.fn()} } }));
it('impede envio de despesa quando o rateio não fecha', async () => {
  render(<AppProvider><ToastProvider><ExpenseForm onDone={() => undefined} /></ToastProvider></AppProvider>);
  await screen.findByText('Pessoa A');
  await userEvent.type(screen.getByLabelText('Descrição'), 'Mercado');
  await userEvent.selectOptions(screen.getByLabelText('Categoria'), '1');
  await userEvent.type(screen.getByLabelText('Valor'), '100,00');
  await userEvent.click(screen.getByLabelText('Dividir'));
  await userEvent.type(screen.getByLabelText('Pessoa A'), '40,00');
  await userEvent.type(screen.getByLabelText('Pessoa B'), '50,00');
  await userEvent.click(screen.getByRole('button', { name: 'Salvar despesa' }));
  expect(await screen.findByRole('alert')).toHaveTextContent(/somar exatamente/);
});
