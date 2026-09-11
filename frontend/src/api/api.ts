import { json, query, request } from './client';
import type { BankAccount, Bill, Category, CreditCard, Expense, Income, Invoice, MonthlyOverview, OverviewBill, OverviewInvoice, Person, Settlement } from './types';

export const api = {
  people: {
    list: (active?: boolean) => request<Person[]>(`/people${query({ active })}`),
    create: (body: { name: string; type: string }) => request<Person>('/people', json('POST', body)),
    status: (id: number, active: boolean) => request<Person>(`/people/${id}/status`, json('PATCH', { active })),
  },
  categories: {
    list: (type?: string, active?: boolean) => request<Category[]>(`/categories${query({ type, active })}`),
    create: (body: { name: string; type: string }) => request<Category>('/categories', json('POST', body)),
    status: (id: number, active: boolean) => request<Category>(`/categories/${id}/status`, json('PATCH', { active })),
  },
  accounts: {
    list: (active?: boolean) => request<BankAccount[]>(`/accounts${query({ active })}`),
    create: (body: unknown) => request<BankAccount>('/accounts', json('POST', body)),
    status: (id: number, active: boolean) => request<BankAccount>(`/accounts/${id}/status`, json('PATCH', { active })),
  },
  cards: {
    list: (active?: boolean) => request<CreditCard[]>(`/cards${query({ active })}`),
    create: (body: unknown) => request<CreditCard>('/cards', json('POST', body)),
    status: (id: number, active: boolean) => request<CreditCard>(`/cards/${id}/status`, json('PATCH', { active })),
    limit: (id: number, creditLimitCents: number) => request<CreditCard>(`/cards/${id}/limit`, json('PATCH', { creditLimitCents })),
  },
  bills: {
    list: (year: number, month: number, status?: string) => request<Bill[]>(`/bills${query({ referenceYear: year, referenceMonth: month, status })}`),
    create: (body: unknown) => request<Bill>('/bills', json('POST', body)),
    amount: (id: number, body: unknown) => request<Bill>(`/bills/${id}/amount`, json('PATCH', body)),
    cancel: (id: number) => request<Bill>(`/bills/${id}/cancel`, json('PATCH')),
  },
  invoices: {
    list: (year?: number, month?: number, creditCardId?: number) => request<Invoice[]>(`/invoices${query({ referenceYear: year, referenceMonth: month, creditCardId })}`),
    create: (body: unknown) => request<Invoice>('/invoices', json('POST', body)),
    close: (id: number) => request<Invoice>(`/invoices/${id}/close`, json('PATCH')),
  },
  expenses: {
    list: (from: string, to: string, creditCardInvoiceId?: number) => request<Expense[]>(`/expenses${query({ from, to, creditCardInvoiceId })}`),
    create: (body: unknown) => request<Expense>('/expenses', json('POST', body)),
  },
  incomes: {
    list: (from: string, to: string) => request<Income[]>(`/incomes${query({ from, to })}`),
    create: (body: unknown) => request<Income>('/incomes', json('POST', body)),
    receive: (id: number, body: unknown) => request<Income>(`/incomes/${id}/receive`, json('PATCH', body)),
    cancel: (id: number) => request<Income>(`/incomes/${id}/cancel`, json('PATCH')),
  },
  payments: { create: (body: unknown) => request('/payments', json('POST', body)) },
  settlements: {
    list: (from: string, to: string) => request<Settlement[]>(`/settlements${query({ from, to })}`),
    create: (body: unknown) => request<Settlement>('/settlements', json('POST', body)),
  },
  overview: {
    monthly: (year: number, month: number) => request<MonthlyOverview>(`/overview/monthly${query({ year, month })}`),
    bills: (year: number, month: number) => request<OverviewBill[]>(`/overview/monthly/bills${query({ year, month })}`),
    invoices: (year: number, month: number) => request<OverviewInvoice[]>(`/overview/monthly/invoices${query({ year, month })}`),
  },
};
