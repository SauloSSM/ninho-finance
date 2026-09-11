import { useMonth } from '../context/AppContext';
const names = ['Jan','Fev','Mar','Abr','Mai','Jun','Jul','Ago','Set','Out','Nov','Dez'];
export function MonthSelector() {
  const { year, month, previousMonth, nextMonth } = useMonth();
  return <div className="month-selector" aria-label="Selecionar mês"><button aria-label="Mês anterior" onClick={previousMonth}>‹</button><span aria-live="polite">{names[month - 1]} {year}</span><button aria-label="Próximo mês" onClick={nextMonth}>›</button></div>;
}
export const longMonth = (year: number, month: number) => new Intl.DateTimeFormat('pt-BR', { month: 'long', year: 'numeric' }).format(new Date(year, month - 1, 1));
