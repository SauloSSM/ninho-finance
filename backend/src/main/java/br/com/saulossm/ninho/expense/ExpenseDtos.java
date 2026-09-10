package br.com.saulossm.ninho.expense;

import br.com.saulossm.ninho.card.CreditCardDtos;
import br.com.saulossm.ninho.category.CategoryDtos;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;
import java.util.List;

public final class ExpenseDtos {

    private ExpenseDtos() {
    }

    public record AllocationRequest(
            @NotNull @Positive Long personId,
            @NotNull @Positive Long amountCents
    ) {
    }

    public record CreateRequest(
            @NotBlank String description,
            @NotNull @Positive Long categoryId,
            @NotNull @Positive Long amountCents,
            @NotNull ExpenseScope scope,
            @NotNull LocalDateTime occurredAt,
            @Positive Long creditCardInvoiceId,
            String notes,
            @NotEmpty List<@Valid AllocationRequest> allocations
    ) {
    }

    public record AllocationResponse(Long personId, String personName, Long amountCents) {
        static AllocationResponse from(ExpenseAllocation allocation) {
            return new AllocationResponse(
                    allocation.getPerson().getId(),
                    allocation.getPerson().getName(),
                    allocation.getAmountCents()
            );
        }
    }

    public record InvoiceSummary(Long id, Long cardId, String cardName) {
        static InvoiceSummary from(br.com.saulossm.ninho.card.CreditCardInvoice invoice) {
            if (invoice == null) {
                return null;
            }
            CreditCardDtos.Summary card = CreditCardDtos.Summary.from(invoice.getCreditCard());
            return new InvoiceSummary(invoice.getId(), card.id(), card.name());
        }
    }

    public record Response(
            Long id,
            String description,
            CategoryDtos.Summary category,
            Long amountCents,
            ExpenseScope scope,
            LocalDateTime occurredAt,
            InvoiceSummary creditCardInvoice,
            String notes,
            List<AllocationResponse> allocations,
            LocalDateTime createdAt
    ) {
    }
}
