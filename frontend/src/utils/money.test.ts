import { describe, expect, it } from 'vitest';
import { formatCurrency, parseCurrencyToCents, validateAllocation } from './money';

describe('dinheiro', () => {
  it('formata centavos como BRL sem float', () => {
    expect(formatCurrency(123456)).toBe('R$ 1.234,56');
    expect(formatCurrency(-99)).toBe('-R$ 0,99');
  });
  it('converte reais digitados para centavos', () => {
    expect(parseCurrencyToCents('1.234,56')).toBe(123456);
    expect(parseCurrencyToCents('R$ 10,5')).toBe(1050);
  });
  it('rejeita uma divisão que não soma o total', () => {
    expect(validateAllocation(10000, [4000, 5000])).toMatch(/somar exatamente/);
    expect(validateAllocation(10000, [4000, 6000])).toBeNull();
  });
});
