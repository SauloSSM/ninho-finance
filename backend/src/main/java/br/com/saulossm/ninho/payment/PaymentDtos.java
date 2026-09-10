package br.com.saulossm.ninho.payment;

import br.com.saulossm.ninho.account.BankAccountDtos;
import br.com.saulossm.ninho.person.PersonDtos;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

public final class PaymentDtos {

    private PaymentDtos() {
    }

    public record CreateRequest(
            @NotNull @Positive Long payerPersonId,
            @Positive Long bankAccountId,
            @NotNull PaymentMethod method,
            @NotNull @Positive Long amountCents,
            @NotNull LocalDateTime paidAt,
            @Positive Long expenseId,
            @Positive Long householdBillId,
            @Positive Long creditCardInvoiceId,
            String notes
    ) {
        @AssertTrue(message = "payment must have exactly one target")
        public boolean hasExactlyOneTarget() {
            int targets = 0;
            targets += expenseId == null ? 0 : 1;
            targets += householdBillId == null ? 0 : 1;
            targets += creditCardInvoiceId == null ? 0 : 1;
            return targets == 1;
        }
    }

    public record Response(
            Long id,
            PersonDtos.Summary payerPerson,
            BankAccountDtos.Summary bankAccount,
            PaymentMethod method,
            Long amountCents,
            LocalDateTime paidAt,
            Long expenseId,
            Long householdBillId,
            Long creditCardInvoiceId,
            String notes,
            LocalDateTime createdAt
    ) {
        static Response from(Payment payment) {
            return new Response(
                    payment.getId(),
                    PersonDtos.Summary.from(payment.getPayerPerson()),
                    BankAccountDtos.Summary.from(payment.getBankAccount()),
                    payment.getMethod(),
                    payment.getAmountCents(),
                    payment.getPaidAt(),
                    payment.getExpense() == null ? null : payment.getExpense().getId(),
                    payment.getHouseholdBill() == null ? null : payment.getHouseholdBill().getId(),
                    payment.getCreditCardInvoice() == null ? null : payment.getCreditCardInvoice().getId(),
                    payment.getNotes(),
                    payment.getCreatedAt()
            );
        }
    }
}
