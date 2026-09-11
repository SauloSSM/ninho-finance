import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { vi } from 'vitest';
import { AppProvider } from '../context/AppContext';
import { DashboardPage } from './DashboardPage';
vi.mock('../api/api', () => ({ api:{ overview:{ monthly:vi.fn().mockRejectedValue(new Error('Backend indisponível')), bills:vi.fn(), invoices:vi.fn() } } }));
it('exibe estado de erro quando o backend não responde', async () => {
  render(<MemoryRouter><AppProvider><DashboardPage /></AppProvider></MemoryRouter>);
  expect(await screen.findByText('Não foi possível carregar')).toBeInTheDocument();
  expect(screen.getByText('Backend indisponível')).toBeInTheDocument();
});
