import { api } from '../api/api';
import { useLoad } from '../hooks/useLoad';
import { formatCurrency } from '../utils/money';
import { formatDate } from '../utils/date';
import { friendlyLabel } from '../utils/labels';
import { EmptyState, ErrorState, LoadingState } from './ui';

export function PaymentHistory({ target, id, amountCents }: { target: 'expenseId' | 'householdBillId' | 'creditCardInvoiceId'; id: number; amountCents?: number }) {
  const state = useLoad(() => api.payments.list(), [target, id]);
  if (state.loading) return <LoadingState />;
  if (state.error) return <ErrorState message={`Não foi possível consultar os pagamentos. ${state.error}`} retry={state.reload} />;
  const payments = (state.data ?? []).filter(payment => payment[target] === id).sort((a, b) => b.paidAt.localeCompare(a.paidAt) || b.id - a.id);
  const paid = payments.reduce((sum, payment) => sum + payment.amountCents, 0);
  return <section aria-label="Histórico de pagamentos"><h3>Pagamentos registrados</h3><p>Pago: {formatCurrency(paid)}{amountCents != null && <> · Restante: {formatCurrency(Math.max(amountCents - paid, 0))}</>}</p>{payments.length ? <ul className="row-list">{payments.map(payment => <li className="data-row" key={payment.id}><strong>{formatCurrency(payment.amountCents)}</strong><span>{formatDate(payment.paidAt)}</span><div><span>{payment.payerPerson.name}</span><span className="row-secondary">{friendlyLabel(payment.method)}{payment.bankAccount && ` · ${payment.bankAccount.name}`}</span></div></li>)}</ul> : <EmptyState title="Nenhum pagamento registrado" text="Os pagamentos deste item aparecerão aqui após o registro." />}</section>;
}
