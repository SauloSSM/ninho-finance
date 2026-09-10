package br.com.saulossm.ninho.income;

import br.com.saulossm.ninho.account.BankAccount;
import br.com.saulossm.ninho.account.BankAccountRepository;
import br.com.saulossm.ninho.category.Category;
import br.com.saulossm.ninho.category.CategoryRepository;
import br.com.saulossm.ninho.category.CategoryType;
import br.com.saulossm.ninho.person.Person;
import br.com.saulossm.ninho.person.PersonRepository;
import br.com.saulossm.ninho.person.PersonType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class IncomeRepositoryTest {

    private static final Path DATABASE = createTemporaryDatabase();

    @Autowired
    private IncomeRepository repository;

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BankAccountRepository accountRepository;

    @Autowired
    private EntityManager entityManager;

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + DATABASE);
    }

    @Test
    void persistsExpectedIncomeWithPersonCategoryAndOptionalAccount() {
        var person = personRepository.save(new Person("Mãe", PersonType.USER));
        var category = categoryRepository.save(new Category("Salário", CategoryType.INCOME));
        var account = accountRepository.saveAndFlush(new BankAccount("Caixa", "Caixa", person, 0L));

        var income = repository.saveAndFlush(new Income(
                person, account, category, "Salário setembro", 250000L, LocalDate.of(2026, 9, 5)
        ));
        entityManager.clear();

        var persisted = repository.findById(income.getId()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(IncomeStatus.EXPECTED);
        assertThat(persisted.getPerson().getId()).isEqualTo(person.getId());
        assertThat(persisted.getCategory().getId()).isEqualTo(category.getId());
        assertThat(persisted.getBankAccount().getId()).isEqualTo(account.getId());
        assertThat(persisted.getReceivedAt()).isNull();
        assertThat(persisted.getCreatedAt()).isNotNull();
    }

    @Test
    void marksExpectedIncomeAsReceivedWithTimestamp() {
        var data = incomeData("Salário");
        var income = repository.saveAndFlush(new Income(
                data.person(), null, data.category(), "Salário", 250000L, LocalDate.of(2026, 9, 5)
        ));
        var receivedAt = LocalDateTime.of(2026, 9, 5, 8, 30);

        income.markReceived(receivedAt);
        repository.flush();
        entityManager.clear();

        var received = repository.findById(income.getId()).orElseThrow();
        assertThat(received.getStatus()).isEqualTo(IncomeStatus.RECEIVED);
        assertThat(received.getReceivedAt()).isEqualTo(receivedAt);
    }

    @Test
    void cancelsOnlyExpectedIncome() {
        var data = incomeData("Ajuda");
        var cancelled = repository.saveAndFlush(new Income(
                data.person(), null, data.category(), "Ajuda", 50000L, LocalDate.of(2026, 9, 10)
        ));
        cancelled.cancel();
        repository.flush();

        assertThat(cancelled.getStatus()).isEqualTo(IncomeStatus.CANCELLED);
        assertThatThrownBy(() -> cancelled.markReceived(LocalDateTime.now()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsNonPositiveAmountAndMissingReceiptTimestamp() {
        var person = new Person("Saulo", PersonType.USER);
        var category = new Category("Ajuda", CategoryType.INCOME);

        assertThatThrownBy(() -> new Income(
                person, null, category, "Ajuda", 0L, LocalDate.of(2026, 9, 10)
        )).isInstanceOf(IllegalArgumentException.class).hasMessage("amountCents must be positive");
        var income = new Income(person, null, category, "Ajuda", 1L, LocalDate.of(2026, 9, 10));
        assertThatThrownBy(() -> income.markReceived(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("receivedAt is required");
    }

    @Test
    void rejectsExpenseCategoryAndAcceptsIncomeCategory() {
        var person = new Person("Saulo", PersonType.USER);
        var expenseCategory = new Category("Salário incorreto", CategoryType.EXPENSE);
        var incomeCategory = new Category("Salário", CategoryType.INCOME);

        assertThatThrownBy(() -> new Income(
                person, null, expenseCategory, "Salário", 100L, LocalDate.of(2026, 9, 10)
        )).isInstanceOf(IllegalArgumentException.class).hasMessage("category must have type INCOME");

        assertThat(new Income(
                person, null, incomeCategory, "Salário", 100L, LocalDate.of(2026, 9, 10)
        ).getCategory()).isSameAs(incomeCategory);
    }

    @Test
    void queriesByPersonStatusAndExpectedDateRange() {
        var mother = personRepository.save(new Person("Mãe", PersonType.USER));
        var saulo = personRepository.save(new Person("Saulo", PersonType.USER));
        var category = categoryRepository.saveAndFlush(new Category("Renda", CategoryType.INCOME));
        var august = repository.save(new Income(
                mother, null, category, "Agosto", 100L, LocalDate.of(2026, 8, 5)
        ));
        var september = repository.save(new Income(
                mother, null, category, "Setembro", 100L, LocalDate.of(2026, 9, 5)
        ));
        september.markReceived(LocalDateTime.of(2026, 9, 5, 9, 0));
        var october = repository.saveAndFlush(new Income(
                saulo, null, category, "Outubro", 100L, LocalDate.of(2026, 10, 5)
        ));

        assertThat(repository.findAllByPerson(mother)).containsExactlyInAnyOrder(august, september);
        assertThat(repository.findAllByStatus(IncomeStatus.RECEIVED)).containsExactly(september);
        assertThat(repository.findAllByExpectedDateBetween(
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)
        )).containsExactly(september).doesNotContain(august, october);
    }

    private IncomeData incomeData(String categoryName) {
        var person = personRepository.save(new Person("Pessoa " + categoryName, PersonType.USER));
        var category = categoryRepository.saveAndFlush(new Category(categoryName, CategoryType.INCOME));
        return new IncomeData(person, category);
    }

    private record IncomeData(Person person, Category category) {
    }

    private static Path createTemporaryDatabase() {
        try {
            var database = Files.createTempFile("ninho-income-repository-", ".db");
            database.toFile().deleteOnExit();
            return database;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not create the test database", exception);
        }
    }
}
