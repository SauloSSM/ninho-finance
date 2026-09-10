package br.com.saulossm.ninho;

import br.com.saulossm.ninho.category.Category;
import br.com.saulossm.ninho.category.CategoryRepository;
import br.com.saulossm.ninho.category.CategoryType;
import br.com.saulossm.ninho.expense.Expense;
import br.com.saulossm.ninho.expense.ExpenseRepository;
import br.com.saulossm.ninho.expense.ExpenseScope;
import br.com.saulossm.ninho.person.Person;
import br.com.saulossm.ninho.person.PersonRepository;
import br.com.saulossm.ninho.person.PersonType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class FinancialForeignKeyIntegrityTest {

    private static final Path DATABASE = createTemporaryDatabase();
    private static final long MISSING_ID = Long.MAX_VALUE;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + DATABASE);
    }

    @Test
    void enforcesForeignKeysAcrossAllNewFinancialTables() {
        var person = personRepository.save(new Person("Saulo", PersonType.USER));
        var category = categoryRepository.save(new Category("Casa", CategoryType.EXPENSE));
        var expense = expenseRepository.saveAndFlush(new Expense(
                "Mercado", category, 100L, ExpenseScope.HOUSEHOLD, LocalDateTime.now(), null, null
        ));

        assertThat(jdbcTemplate.queryForObject("PRAGMA foreign_keys", Integer.class)).isEqualTo(1);

        assertForeignKeyFailure(
                """
                INSERT INTO credit_cards
                    (name, institution, holder_id, invoice_payer_id, default_responsible_person_id,
                     credit_limit_cents, closing_day, due_day)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                "Inválido", "Banco", MISSING_ID, person.getId(), person.getId(), 0L, 1, 1
        );
        assertForeignKeyFailure(
                """
                INSERT INTO credit_card_invoices
                    (credit_card_id, reference_year, reference_month, due_date, status)
                VALUES (?, ?, ?, ?, ?)
                """,
                MISSING_ID, 2026, 9, "2026-09-10", "OPEN"
        );
        assertForeignKeyFailure(
                """
                INSERT INTO incomes
                    (person_id, category_id, description, amount_cents, expected_date, status)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                MISSING_ID, category.getId(), "Inválida", 100L, "2026-09-10", "EXPECTED"
        );
        assertForeignKeyFailure(
                """
                INSERT INTO household_bills
                    (name, category_id, responsible_person_id, reference_year, reference_month,
                     expected_amount_cents, due_date, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                "Inválida", category.getId(), MISSING_ID, 2026, 9, 100L, "2026-09-10", "PENDING"
        );
        assertForeignKeyFailure(
                """
                INSERT INTO expenses
                    (description, category_id, amount_cents, scope, occurred_at)
                VALUES (?, ?, ?, ?, ?)
                """,
                "Inválida", MISSING_ID, 100L, "PERSONAL", "2026-09-10 12:00:00"
        );
        assertForeignKeyFailure(
                """
                INSERT INTO expense_allocations (expense_id, person_id, amount_cents)
                VALUES (?, ?, ?)
                """,
                expense.getId(), MISSING_ID, 100L
        );
        assertForeignKeyFailure(
                """
                INSERT INTO payments
                    (payer_person_id, bank_account_id, method, amount_cents, paid_at, expense_id)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                person.getId(), MISSING_ID, "PIX", 100L, "2026-09-10 12:00:00", expense.getId()
        );
        assertForeignKeyFailure(
                """
                INSERT INTO settlement_payments
                    (from_person_id, to_person_id, amount_cents, paid_at)
                VALUES (?, ?, ?, ?)
                """,
                person.getId(), MISSING_ID, 100L, "2026-09-10 12:00:00"
        );
    }

    private void assertForeignKeyFailure(String sql, Object... arguments) {
        assertThatThrownBy(() -> jdbcTemplate.update(sql, arguments))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("FOREIGN KEY constraint failed");
    }

    private static Path createTemporaryDatabase() {
        try {
            var database = Files.createTempFile("ninho-financial-foreign-keys-", ".db");
            database.toFile().deleteOnExit();
            return database;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not create the test database", exception);
        }
    }
}
