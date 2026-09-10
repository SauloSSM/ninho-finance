package br.com.saulossm.ninho.card;

import br.com.saulossm.ninho.person.PersonDtos;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDateTime;

public final class CreditCardDtos {

    private CreditCardDtos() {
    }

    public record CreateRequest(
            @NotBlank String name,
            @NotBlank String institution,
            @NotNull @Positive Long holderId,
            @NotNull @Positive Long invoicePayerId,
            @NotNull @Positive Long defaultResponsiblePersonId,
            @Pattern(regexp = "[0-9]{4}") String lastFour,
            @NotNull @PositiveOrZero Long creditLimitCents,
            @NotNull @Min(1) @Max(31) Integer closingDay,
            @NotNull @Min(1) @Max(31) Integer dueDay
    ) {
    }

    public record StatusRequest(@NotNull Boolean active) {
    }

    public record LimitRequest(@NotNull @PositiveOrZero Long creditLimitCents) {
    }

    public record Response(
            Long id,
            String name,
            String institution,
            PersonDtos.Summary holder,
            PersonDtos.Summary invoicePayer,
            PersonDtos.Summary defaultResponsiblePerson,
            String lastFour,
            Long creditLimitCents,
            Integer closingDay,
            Integer dueDay,
            boolean active,
            LocalDateTime createdAt
    ) {
        static Response from(CreditCard card) {
            return new Response(
                    card.getId(),
                    card.getName(),
                    card.getInstitution(),
                    PersonDtos.Summary.from(card.getHolder()),
                    PersonDtos.Summary.from(card.getInvoicePayer()),
                    PersonDtos.Summary.from(card.getDefaultResponsiblePerson()),
                    card.getLastFour(),
                    card.getCreditLimitCents(),
                    card.getClosingDay(),
                    card.getDueDay(),
                    card.isActive(),
                    card.getCreatedAt()
            );
        }
    }

    public record Summary(Long id, String name) {
        public static Summary from(CreditCard card) {
            return new Summary(card.getId(), card.getName());
        }
    }
}
