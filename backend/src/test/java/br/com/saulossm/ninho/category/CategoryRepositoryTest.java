package br.com.saulossm.ninho.category;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CategoryRepositoryTest {

    private static final Path DATABASE = createTemporaryDatabase();

    @Autowired
    private CategoryRepository repository;

    @Autowired
    private EntityManager entityManager;

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + DATABASE);
    }

    @Test
    void persistsCategoryWithDatabaseGeneratedValues() {
        var category = repository.saveAndFlush(new Category("Groceries", CategoryType.EXPENSE));

        assertThat(category.getId()).isNotNull();
        assertThat(category.getName()).isEqualTo("Groceries");
        assertThat(category.getType()).isEqualTo(CategoryType.EXPENSE);
        assertThat(category.isActive()).isTrue();
        assertThat(category.getCreatedAt()).isNotNull();
    }

    @Test
    void persistsRenameAndActivationChanges() {
        var category = repository.saveAndFlush(new Category("Salary", CategoryType.INCOME));

        category.rename("Monthly salary");
        category.deactivate();
        repository.flush();
        entityManager.clear();

        var inactiveCategory = repository.findById(category.getId()).orElseThrow();
        assertThat(inactiveCategory.getName()).isEqualTo("Monthly salary");
        assertThat(inactiveCategory.isActive()).isFalse();

        inactiveCategory.activate();
        repository.flush();
        entityManager.clear();

        assertThat(repository.findById(category.getId()).orElseThrow().isActive()).isTrue();
    }

    @Test
    void findsOnlyActiveCategories() {
        var activeExpense = repository.save(new Category("Rent", CategoryType.EXPENSE));
        var activeIncome = repository.save(new Category("Bonus", CategoryType.INCOME));
        var inactiveExpense = new Category("Subscriptions", CategoryType.EXPENSE);
        inactiveExpense.deactivate();
        repository.saveAndFlush(inactiveExpense);

        assertThat(repository.findAllByActiveTrue())
                .containsExactlyInAnyOrder(activeExpense, activeIncome)
                .doesNotContain(inactiveExpense);
    }

    @Test
    void findsOnlyActiveCategoriesByType() {
        var activeExpense = repository.save(new Category("Transport", CategoryType.EXPENSE));
        var inactiveExpense = new Category("Leisure", CategoryType.EXPENSE);
        inactiveExpense.deactivate();
        repository.save(inactiveExpense);
        var activeIncome = repository.save(new Category("Dividends", CategoryType.INCOME));
        repository.flush();

        assertThat(repository.findAllByTypeAndActiveTrue(CategoryType.EXPENSE))
                .containsExactly(activeExpense)
                .doesNotContain(inactiveExpense, activeIncome);
        assertThat(repository.findAllByTypeAndActiveTrue(CategoryType.INCOME))
                .containsExactly(activeIncome)
                .doesNotContain(activeExpense, inactiveExpense);
    }

    @Test
    void allowsSameNameForDifferentTypes() {
        var expense = repository.save(new Category("Adjustment", CategoryType.EXPENSE));
        var income = repository.saveAndFlush(new Category("Adjustment", CategoryType.INCOME));

        assertThat(expense.getId()).isNotNull();
        assertThat(income.getId()).isNotNull();
    }

    @Test
    void rejectsDuplicateNameAndType() {
        repository.saveAndFlush(new Category("Food", CategoryType.EXPENSE));

        assertThatThrownBy(() -> repository.saveAndFlush(new Category("Food", CategoryType.EXPENSE)))
                .isInstanceOf(JpaSystemException.class)
                .hasMessageContaining("UNIQUE constraint failed: categories.name, categories.type");
    }

    @Test
    void rejectsInvalidRequiredFields() {
        assertThatThrownBy(() -> new Category(null, CategoryType.EXPENSE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("name is required");
        assertThatThrownBy(() -> new Category(" ", CategoryType.EXPENSE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("name is required");
        assertThatThrownBy(() -> new Category("Food", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("type is required");
    }

    private static Path createTemporaryDatabase() {
        try {
            var database = Files.createTempFile("ninho-category-repository-", ".db");
            database.toFile().deleteOnExit();
            return database;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not create the test database", exception);
        }
    }
}
