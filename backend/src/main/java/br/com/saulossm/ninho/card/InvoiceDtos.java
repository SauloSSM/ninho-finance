package br.com.saulossm.ninho.card;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class InvoiceDtos {

    private InvoiceDtos() {
    }

    public record CreateRequest(
            @NotNull @Positive Long creditCardId,
            @NotNull @Positive Integer referenceYear,
            @NotNull @Min(1) @Max(12) Integer referenceMonth,
            @NotNull LocalDate dueDate,
            @PositiveOrZero Long reportedTotalCents
    ) {
    }

    public record Response(
            Long id,
            CreditCardDtos.Summary creditCard,
            Integer referenceYear,
            Integer referenceMonth,
            LocalDate dueDate,
            InvoiceStatus status,
            Long reportedTotalCents,
            Long calculatedTotalCents,
            Long paidAmountCents,
            Long outstandingAmountCents,
            Boolean reconciled,
            LocalDateTime createdAt
    ) {
    }
}
