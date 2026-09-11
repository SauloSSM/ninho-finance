import { useState } from 'react';
import { BillForm, ExpenseForm, IncomeForm, PaymentForm, SettlementForm } from './forms';
import { Modal } from './ui';
type Kind = 'bill' | 'expense' | 'income' | 'payment' | 'settlement';
const options: Array<{ kind: Kind; title: string; text: string }> = [
  { kind:'bill', title:'Conta da casa', text:'Água, luz, internet, telefone e outras contas.' },
  { kind:'expense', title:'Despesa', text:'Compras, mercado, farmácia, combustível e outros gastos.' },
  { kind:'income', title:'Renda', text:'Salário, ajuda recebida ou qualquer outra entrada.' },
  { kind:'payment', title:'Pagamento', text:'Marque uma conta, despesa ou fatura como paga.' },
  { kind:'settlement', title:'Acerto', text:'Registre uma transferência entre duas pessoas.' },
];
export function RegistryModal({ onClose }: { onClose: () => void }) {
  const [kind, setKind] = useState<Kind>(); const selected = options.find((item) => item.kind === kind);
  return <Modal title={selected?.title ?? 'Registrar'} subtitle={selected ? 'Preencha os dados abaixo.' : 'O que você quer adicionar agora?'} onClose={onClose} small={!kind}>{!kind ? <div className="register-options">{options.map((option) => <button className="register-option" onClick={() => setKind(option.kind)} key={option.kind}><div><strong>{option.title}</strong><span>{option.text}</span></div><b>›</b></button>)}</div> : kind === 'bill' ? <BillForm onDone={onClose} /> : kind === 'expense' ? <ExpenseForm onDone={onClose} /> : kind === 'income' ? <IncomeForm onDone={onClose} /> : kind === 'payment' ? <PaymentForm onDone={onClose} /> : <SettlementForm onDone={onClose} />}</Modal>;
}
