const labels: Record<string, string> = {
  PENDING: 'Pendente', PARTIALLY_PAID: 'Parcialmente paga', PAID: 'Paga', CANCELLED: 'Cancelada',
  OPEN: 'Aberta', CLOSED: 'Fechada', EXPECTED: 'Prevista', RECEIVED: 'Recebida',
  EXPENSE: 'Despesa', INCOME: 'Renda', HOUSEHOLD: 'Casa', PERSONAL: 'Pessoal',
  USER: 'Usuário', EXTERNAL: 'Contato externo', PIX: 'Pix', BOLETO: 'Boleto', DEBIT: 'Débito', CASH: 'Dinheiro', TRANSFER: 'Transferência', OTHER: 'Outro',
};
export const friendlyLabel = (value?: string | null) => value ? (labels[value] ?? value) : '—';
