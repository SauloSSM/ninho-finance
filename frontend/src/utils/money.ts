export function formatCurrency(cents: number | null | undefined): string {
  const safe = Number.isFinite(cents) ? Number(cents) : 0;
  const sign = safe < 0 ? '-' : '';
  const absolute = Math.abs(Math.trunc(safe));
  const reais = Math.floor(absolute / 100).toLocaleString('pt-BR');
  return `${sign}R$ ${reais},${String(absolute % 100).padStart(2, '0')}`;
}

export function parseCurrencyToCents(value: string): number {
  const clean = value.trim().replace(/R\$/gi, '').replace(/\s/g, '');
  if (!clean) return 0;
  const negative = clean.startsWith('-');
  const unsigned = clean.replace(/^[+-]/, '');
  if (!/^\d{1,3}(\.\d{3})*(,\d{0,2})?$|^\d+(,\d{0,2})?$/.test(unsigned)) {
    throw new Error('Informe um valor válido em reais.');
  }
  const [whole, fraction = ''] = unsigned.replace(/\./g, '').split(',');
  const cents = Number(whole) * 100 + Number(fraction.padEnd(2, '0'));
  if (!Number.isSafeInteger(cents)) throw new Error('O valor informado é muito alto.');
  return negative ? -cents : cents;
}

export function validateAllocation(totalCents: number, values: number[]): string | null {
  if (values.some((value) => !Number.isInteger(value) || value <= 0)) return 'Informe valores maiores que zero para cada pessoa.';
  return values.reduce((sum, value) => sum + value, 0) === totalCents
    ? null
    : 'A divisão precisa somar exatamente o valor total.';
}
