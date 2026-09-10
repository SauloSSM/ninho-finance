package br.com.saulossm.ninho.income;

import br.com.saulossm.ninho.account.BankAccountDtos;
import br.com.saulossm.ninho.category.CategoryDtos;
import br.com.saulossm.ninho.person.PersonDtos;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class IncomeDtos {

    private IncomeDtos() {
    }

    public record CreateRequest(
            @NotNull @Positive Long personId,
            @Positive Long bankAccountId,
            @NotNull @Positive Long categoryId,
            @NotBlank String description,
            @NotNull @Positive Long amountCents,
            @NotNull LocalDate expectedDate
    ) {
    }

    public record ReceiveRequest(@NotNull LocalDateTime receivedAt, @Positive Long bankAccountId) {
    }

    public record Response(
            Long id,
            PersonDtos.Summary person,
            BankAccountDtos.Summary bankAccount,
            CategoryDtos.Summary category,
            String description,
            Long amountCents,
            LocalDate expectedDate,
            LocalDateTime receivedAt,
            IncomeStatus status,
            LocalDateTime createdAt
    ) {
        static Response from(Income income) {
            return new Response(
                    income.getId(),
                    PersonDtos.Summary.from(income.getPerson()),
                    BankAccountDtos.Summary.from(income.getBankAccount()),
                    CategoryDtos.Summary.from(income.getCategory()),
                    income.getDescription(),
                    income.getAmountCents(),
                    income.getExpectedDate(),
                    income.getReceivedAt(),
                    income.getStatus(),
                    income.getCreatedAt()
            );
        }
    }
}
