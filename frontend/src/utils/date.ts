export function formatDate(value?: string | null): string {
  if (!value) return '—';
  const [date] = value.split('T');
  const [year, month, day] = date.split('-');
  return year && month && day ? `${day}/${month}/${year}` : value;
}

export function monthRange(year: number, month: number) {
  const lastDay = new Date(year, month, 0).getDate();
  return { from: `${year}-${String(month).padStart(2, '0')}-01`, to: `${year}-${String(month).padStart(2, '0')}-${lastDay}` };
}

export function toLocalDateTime(date: string): string { return `${date}T12:00:00`; }
export function todayIso(): string { return new Date().toISOString().slice(0, 10); }
