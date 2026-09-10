package br.com.saulossm.ninho.bill;

import br.com.saulossm.ninho.category.CategoryDtos;
import br.com.saulossm.ninho.person.PersonDtos;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class HouseholdBillDtos {

    private HouseholdBillDtos() {
    }

    public record CreateRequest(
            @NotBlank String name,
            @NotNull @Positive Long categoryId,
            @NotNull @Positive Long responsiblePersonId,
            @NotNull @Positive Integer referenceYear,
            @NotNull @Min(1) @Max(12) Integer referenceMonth,
            @NotNull @PositiveOrZero Long expectedAmountCents,
            @PositiveOrZero Long actualAmountCents,
            @NotNull LocalDate dueDate
    ) {
    }

    public record AmountRequest(
            @PositiveOrZero Long expectedAmountCents,
            @PositiveOrZero Long actualAmountCents
    ) {
        @AssertTrue(message = "at least one amount must be provided")
        public boolean isAnyAmountProvided() {
            return expectedAmountCents != null || actualAmountCents != null;
        }
    }

    public record Response(
            Long id,
            String name,
            CategoryDtos.Summary category,
            PersonDtos.Summary responsiblePerson,
            Integer referenceYear,
            Integer referenceMonth,
            Long expectedAmountCents,
            Long actualAmountCents,
            Long effectiveAmountCents,
            Long paidAmountCents,
            Long outstandingAmountCents,
            LocalDate dueDate,
            HouseholdBillStatus status,
            boolean overdue,
            LocalDateTime createdAt
    ) {
    }
}
