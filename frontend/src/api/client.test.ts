import { describe, expect, it } from 'vitest';
import { problemToApiError } from './client';

describe('ProblemDetail', () => {
  it('preserva mensagens de validação e campos', () => {
    const error = problemToApiError({ status: 400, detail: 'Request validation failed', errors: [{ field: 'name', message: 'must not be blank' }] });
    expect(error.message).toBe('must not be blank');
    expect(error.fieldErrors.name).toBe('must not be blank');
  });
  it('não expõe erro interno genérico', () => {
    expect(problemToApiError({ status: 500, detail: 'An unexpected error occurred' }).message).toMatch(/problema inesperado/);
  });
});
