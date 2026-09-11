export type Summary = { id: number; name: string; institution?: string; type?: string };
export type Person = { id: number; name: string; type: 'USER' | 'EXTERNAL'; active: boolean; createdAt: string };
export type Category = { id: number; name: string; type: 'EXPENSE' | 'INCOME'; active: boolean; createdAt: string };
export type BankAccount = { id: number; name: string; institution: string; owner: Summary; initialBalanceCents: number; active: boolean; createdAt: string };
export type CreditCard = { id: number; name: string; institution: string; holder: Summary; invoicePayer: Summary; defaultResponsiblePerson: Summary; lastFour?: string; creditLimitCents: number; closingDay: number; dueDay: number; active: boolean; createdAt: string };
export type BillStatus = 'PENDING' | 'PARTIALLY_PAID' | 'PAID' | 'CANCELLED';
export type Bill = { id: number; name: string; category: Summary; responsiblePerson: Summary; referenceYear: number; referenceMonth: number; expectedAmountCents: number; actualAmountCents?: number; effectiveAmountCents: number; paidAmountCents: number; outstandingAmountCents: number; dueDate: string; status: BillStatus; overdue: boolean; createdAt: string };
export type Invoice = { id: number; creditCard: Summary; referenceYear: number; referenceMonth: number; dueDate: string; status: 'OPEN' | 'CLOSED'; reportedTotalCents?: number; calculatedTotalCents: number; paidAmountCents: number; outstandingAmountCents: number; reconciled?: boolean; createdAt: string };
export type Allocation = { personId: number; personName: string; amountCents: number };
export type Expense = { id: number; description: string; category: Summary; amountCents: number; scope: 'PERSONAL' | 'HOUSEHOLD'; occurredAt: string; creditCardInvoice?: { id: number; cardId: number; cardName: string }; notes?: string; allocations: Allocation[]; createdAt: string };
export type Income = { id: number; person: Summary; bankAccount?: Summary; category: Summary; description: string; amountCents: number; expectedDate: string; receivedAt?: string; status: 'EXPECTED' | 'RECEIVED' | 'CANCELLED'; createdAt: string };
export type Settlement = { id: number; fromPerson: Summary; toPerson: Summary; amountCents: number; paidAt: string; notes?: string; createdAt: string };
export type MonthlyOverview = { year: number; month: number; income: { expectedCents: number; receivedCents: number }; householdBills: { totalCents: number; paidCents: number; outstandingCents: number; overdueCents: number }; creditCards: { totalCents: number; paidCents: number; outstandingCents: number }; expenses: { personalCents: number; householdCents: number; totalCents: number } };
export type OverviewBill = Pick<Bill, 'id' | 'name' | 'effectiveAmountCents' | 'paidAmountCents' | 'outstandingAmountCents' | 'dueDate' | 'status' | 'overdue' | 'responsiblePerson'>;
export type OverviewInvoice = { id: number; cardId: number; cardName: string; dueDate: string; status: string; calculatedTotalCents: number; paidAmountCents: number; outstandingAmountCents: number; reportedTotalCents?: number; reconciled?: boolean };

export type ProblemDetail = { status?: number; title?: string; detail?: string; instance?: string; errors?: Array<{ field: string; message: string }> };
