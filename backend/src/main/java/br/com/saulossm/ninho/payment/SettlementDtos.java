package br.com.saulossm.ninho.payment;

import br.com.saulossm.ninho.person.PersonDtos;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

public final class SettlementDtos {

    private SettlementDtos() {
    }

    public record CreateRequest(
            @NotNull @Positive Long fromPersonId,
            @NotNull @Positive Long toPersonId,
            @NotNull @Positive Long amountCents,
            @NotNull LocalDateTime paidAt,
            String notes
    ) {
    }

    public record Response(
            Long id,
            PersonDtos.Summary fromPerson,
            PersonDtos.Summary toPerson,
            Long amountCents,
            LocalDateTime paidAt,
            String notes,
            LocalDateTime createdAt
    ) {
        static Response from(SettlementPayment settlement) {
            return new Response(
                    settlement.getId(),
                    PersonDtos.Summary.from(settlement.getFromPerson()),
                    PersonDtos.Summary.from(settlement.getToPerson()),
                    settlement.getAmountCents(),
                    settlement.getPaidAt(),
                    settlement.getNotes(),
                    settlement.getCreatedAt()
            );
        }
    }
}
