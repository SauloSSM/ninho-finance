package br.com.saulossm.ninho.overview;

import br.com.saulossm.ninho.bill.HouseholdBillStatus;
import br.com.saulossm.ninho.card.InvoiceStatus;
import br.com.saulossm.ninho.person.PersonDtos;

import java.time.LocalDate;

public final class MonthlyOverviewDtos {

    private MonthlyOverviewDtos() {
    }

    public record Income(Long expectedCents, Long receivedCents) {
    }

    public record HouseholdBills(Long totalCents, Long paidCents, Long outstandingCents, Long overdueCents) {
    }

    public record CreditCards(Long totalCents, Long paidCents, Long outstandingCents) {
    }

    public record Expenses(Long personalCents, Long householdCents, Long totalCents) {
    }

    public record Response(
            Integer year,
            Integer month,
            Income income,
            HouseholdBills householdBills,
            CreditCards creditCards,
            Expenses expenses
    ) {
    }

    public record BillItem(
            Long id,
            String name,
            Long effectiveAmountCents,
            Long paidAmountCents,
            Long outstandingAmountCents,
            LocalDate dueDate,
            HouseholdBillStatus status,
            boolean overdue,
            PersonDtos.Summary responsiblePerson
    ) {
    }

    public record InvoiceItem(
            Long id,
            Long cardId,
            String cardName,
            LocalDate dueDate,
            InvoiceStatus status,
            Long calculatedTotalCents,
            Long paidAmountCents,
            Long outstandingAmountCents,
            Long reportedTotalCents,
            Boolean reconciled
    ) {
    }
}
