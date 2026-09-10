package br.com.saulossm.ninho.person;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public final class PersonDtos {

    private PersonDtos() {
    }

    public record CreateRequest(@NotBlank String name, @NotNull PersonType type) {
    }

    public record StatusRequest(@NotNull Boolean active) {
    }

    public record Response(
            Long id,
            String name,
            PersonType type,
            boolean active,
            LocalDateTime createdAt
    ) {
        static Response from(Person person) {
            return new Response(
                    person.getId(), person.getName(), person.getType(), person.isActive(), person.getCreatedAt()
            );
        }
    }

    public record Summary(Long id, String name) {
        public static Summary from(Person person) {
            return new Summary(person.getId(), person.getName());
        }
    }
}
