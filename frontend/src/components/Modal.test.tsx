import { useState } from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Modal } from './ui';

it('mantém foco ao digitar em ajuste controlado pelo componente pai', async () => {
  function Adjustment() {
    const [value, setValue] = useState('');
    const [open, setOpen] = useState(true);
    return open ? <Modal title="Ajustar" onClose={() => setOpen(false)}><label>Valor<input value={value} onChange={e => setValue(e.target.value)}/></label></Modal> : <span>Fechado</span>;
  }
  render(<Adjustment/>);
  const input = screen.getByLabelText('Valor');
  await userEvent.type(input, '123,45');
  expect(input).toHaveValue('123,45');
  expect(input).toHaveFocus();
  await userEvent.keyboard('{Escape}');
  expect(screen.getByText('Fechado')).toBeInTheDocument();
});
