package br.com.saulossm.ninho.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public final class CategoryDtos {

    private CategoryDtos() {
    }

    public record CreateRequest(@NotBlank String name, @NotNull CategoryType type) {
    }

    public record StatusRequest(@NotNull Boolean active) {
    }

    public record Response(
            Long id,
            String name,
            CategoryType type,
            boolean active,
            LocalDateTime createdAt
    ) {
        static Response from(Category category) {
            return new Response(
                    category.getId(), category.getName(), category.getType(), category.isActive(), category.getCreatedAt()
            );
        }
    }

    public record Summary(Long id, String name, CategoryType type) {
        public static Summary from(Category category) {
            return new Summary(category.getId(), category.getName(), category.getType());
        }
    }
}
