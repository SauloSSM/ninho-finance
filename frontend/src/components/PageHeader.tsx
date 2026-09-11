import { MonthSelector } from './MonthSelector';
import { Button } from './ui';
export function PageHeader({ title, subtitle, actionLabel = '+ Registrar', onAction, showAction = true }: { title: string; subtitle: string; actionLabel?: string; onAction?: () => void; showAction?: boolean }) {
  return <header className="page-header"><div><h1 className="page-title">{title}</h1><p className="page-subtitle">{subtitle}</p></div><MonthSelector />{showAction && <Button variant="primary" className="header-action" onClick={onAction}>{actionLabel}</Button>}</header>;
}
