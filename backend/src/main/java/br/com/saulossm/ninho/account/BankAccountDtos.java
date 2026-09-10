package br.com.saulossm.ninho.account;

import br.com.saulossm.ninho.person.PersonDtos;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDateTime;

public final class BankAccountDtos {

    private BankAccountDtos() {
    }

    public record CreateRequest(
            @NotBlank String name,
            @NotBlank String institution,
            @NotNull @Positive Long ownerId,
            @NotNull @PositiveOrZero Long initialBalanceCents
    ) {
    }

    public record StatusRequest(@NotNull Boolean active) {
    }

    public record Response(
            Long id,
            String name,
            String institution,
            PersonDtos.Summary owner,
            Long initialBalanceCents,
            boolean active,
            LocalDateTime createdAt
    ) {
        static Response from(BankAccount account) {
            return new Response(
                    account.getId(),
                    account.getName(),
                    account.getInstitution(),
                    PersonDtos.Summary.from(account.getOwner()),
                    account.getInitialBalanceCents(),
                    account.isActive(),
                    account.getCreatedAt()
            );
        }
    }

    public record Summary(Long id, String name, String institution) {
        public static Summary from(BankAccount account) {
            return account == null ? null : new Summary(account.getId(), account.getName(), account.getInstitution());
        }
    }
}
