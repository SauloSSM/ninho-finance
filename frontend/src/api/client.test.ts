import { describe, expect, it } from 'vitest';
import { problemToApiError } from './client';

describe('ProblemDetail', () => {
  it('preserva mensagens de validação e campos', () => {
    const error = problemToApiError({ status: 400, detail: 'Request validation failed', errors: [{ field: 'name', message: 'must not be blank' }] });
    expect(error.message).toBe('Nome: Preencha este campo.');
    expect(error.fieldErrors.name).toBe('Preencha este campo.');
  });
  it('não expõe erro interno genérico', () => {
    expect(problemToApiError({ status: 500, detail: 'An unexpected error occurred' }).message).toMatch(/problema inesperado/);
  });
  it.each(['Expense', 'Household bill', 'Invoice'])('traduz pagamento excedente em %s', (target) => {
    expect(problemToApiError({ detail: `${target} payment exceeds outstanding amount` }).message).toBe('O valor informado é maior que o valor restante.');
  });
  it('identifica o campo e preserva o limite para correção', () => {
    expect(problemToApiError({ errors: [{ field: 'dueDay', message: 'must be less than or equal to 31' }] }).message).toBe('Dia de vencimento: Informe um valor igual ou menor que 31.');
  });
});
