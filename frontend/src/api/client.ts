import type { ProblemDetail } from './types';

export const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api').replace(/\/$/, '');

const technicalMessages: Record<string, string> = {
  'Request validation failed': 'Revise os campos informados.',
  'Parameter validation failed': 'Os filtros informados não são válidos.',
  'Malformed or unsupported request value': 'Há um valor inválido no formulário.',
  'An unexpected error occurred': 'O Ninho encontrou um problema inesperado. Tente novamente.',
  'The request conflicts with existing data': 'Este cadastro está sendo usado e não pode ser alterado dessa forma.',
};

export class ApiError extends Error {
  constructor(message: string, public status = 0, public fieldErrors: Record<string, string> = {}) { super(message); }
}

export function problemToApiError(problem: ProblemDetail, fallbackStatus = 0): ApiError {
  const fields = Object.fromEntries((problem.errors ?? []).map(({ field, message }) => [field, message]));
  const validation = problem.errors?.map((error) => error.message).filter(Boolean).join(' ');
  const detail = problem.detail ? (technicalMessages[problem.detail] ?? problem.detail) : '';
  return new ApiError(validation || detail || 'Não foi possível concluir a ação.', problem.status ?? fallbackStatus, fields);
}

export async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  try {
    const response = await fetch(`${API_BASE_URL}${path}`, {
      ...options,
      headers: { ...(options.body ? { 'Content-Type': 'application/json' } : {}), ...options.headers },
    });
    if (!response.ok) {
      let problem: ProblemDetail = { status: response.status };
      try { problem = await response.json() as ProblemDetail; } catch { /* resposta sem JSON */ }
      throw problemToApiError(problem, response.status);
    }
    if (response.status === 204) return undefined as T;
    return await response.json() as T;
  } catch (error) {
    if (error instanceof ApiError) throw error;
    throw new ApiError('Não foi possível falar com o servidor. Verifique se o backend está disponível.');
  }
}

export const json = (method: 'POST' | 'PATCH', body?: unknown): RequestInit => ({ method, ...(body === undefined ? {} : { body: JSON.stringify(body) }) });
export const query = (values: Record<string, string | number | boolean | undefined>) => {
  const params = new URLSearchParams();
  Object.entries(values).forEach(([key, value]) => { if (value !== undefined && value !== '') params.set(key, String(value)); });
  const result = params.toString();
  return result ? `?${result}` : '';
};
