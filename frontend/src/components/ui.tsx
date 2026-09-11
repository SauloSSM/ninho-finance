import { cloneElement, isValidElement, useEffect, useId, useRef, type ButtonHTMLAttributes, type InputHTMLAttributes, type ReactElement, type ReactNode } from 'react';
import { friendlyLabel } from '../utils/labels';

export function Button({ variant = 'default', className = '', ...props }: ButtonHTMLAttributes<HTMLButtonElement> & { variant?: 'default' | 'primary' | 'outline' | 'danger' }) {
  return <button className={`button ${variant} ${className}`} {...props} />;
}
export function StatusBadge({ status, danger }: { status: string; danger?: boolean }) {
  const success = ['PAID', 'RECEIVED', 'ACTIVE'].includes(status);
  const isDanger = danger || status === 'CANCELLED';
  return <span className={`badge ${success ? 'success' : isDanger ? 'danger' : status === 'CLOSED' ? 'neutral' : ''}`}>{friendlyLabel(status)}</span>;
}
export function FormField({ label, error, full, children }: { label: string; error?: string; full?: boolean; children: ReactNode }) {
  const id = useId();
  const child = isValidElement(children) ? children as ReactElement<{ id?: string; 'aria-describedby'?: string }> : null;
  const inputId = child?.props.id ?? id;
  const control = child ? cloneElement(child, { id: inputId, ...(error ? { 'aria-describedby': `${inputId}-error` } : {}) }) : children;
  return <div className={`form-field ${full ? 'full' : ''}`}><label htmlFor={inputId}>{label}</label>{control}{error && <span id={`${inputId}-error`} className="field-error" role="alert">{error}</span>}</div>;
}
export function CurrencyInput({ value, onChange, ...props }: Omit<InputHTMLAttributes<HTMLInputElement>, 'onChange'> & { value: string; onChange: (value: string) => void }) {
  return <input className="input" inputMode="decimal" placeholder="R$ 0,00" value={value} onChange={(event) => onChange(event.target.value)} {...props} />;
}
export function Modal({ title, subtitle, onClose, small, children }: { title: string; subtitle?: string; onClose: () => void; small?: boolean; children: ReactNode }) {
  const ref = useRef<HTMLDivElement>(null);
  useEffect(() => {
    const previous = document.activeElement as HTMLElement | null;
    ref.current?.focus();
    const handler = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onClose();
      if (event.key === 'Tab' && ref.current) {
        const focusable = [...ref.current.querySelectorAll<HTMLElement>('button:not(:disabled), input:not(:disabled), select:not(:disabled), textarea:not(:disabled), [href], [tabindex]:not([tabindex="-1"])')];
        if (!focusable.length) return;
        const first = focusable[0], last = focusable[focusable.length - 1];
        if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last.focus(); }
        else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus(); }
      }
    };
    document.addEventListener('keydown', handler); document.body.style.overflow = 'hidden';
    return () => { document.removeEventListener('keydown', handler); document.body.style.overflow = ''; previous?.focus(); };
  }, [onClose]);
  return <div className="modal-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose(); }}><div className={`modal ${small ? 'small' : ''}`} role="dialog" aria-modal="true" aria-labelledby="modal-title" tabIndex={-1} ref={ref}><header className="modal-header"><div><h2 id="modal-title">{title}</h2>{subtitle && <p>{subtitle}</p>}</div><button className="close-button" aria-label="Fechar" onClick={onClose}>×</button></header>{children}</div></div>;
}
export function LoadingState() { return <div className="state" role="status"><p>Carregando seu Ninho…</p></div>; }
export function ErrorState({ message, retry }: { message: string; retry?: () => void }) { return <div className="state" role="alert"><div><h3>Não foi possível carregar</h3><p>{message}</p>{retry && <Button onClick={retry}>Tentar novamente</Button>}</div></div>; }
export function EmptyState({ title = 'Seu Ninho está começando.', text = 'Cadastre pessoas, contas e cartões para organizar o mês.' }: { title?: string; text?: string }) { return <div className="state"><div><h3>{title}</h3><p>{text}</p></div></div>; }
export function ChoiceTabs({ items, value, onChange, label = 'Opções' }: { items: Array<{ value: string; label: string }>; value: string; onChange: (value: string) => void; label?: string }) { return <div className="tabs" role="tablist" aria-label={label}>{items.map((item) => <button type="button" role="tab" aria-selected={value === item.value} className={`chip ${value === item.value ? 'active' : ''}`} onClick={() => onChange(item.value)} key={item.value}>{item.label}</button>)}</div>; }
