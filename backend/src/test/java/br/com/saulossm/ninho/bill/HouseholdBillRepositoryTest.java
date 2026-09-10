package br.com.saulossm.ninho.bill;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class HouseholdBillRepositoryTest {

    private static final Path DATABASE = createTemporaryDatabase();

    @Autowired
    private HouseholdBillRepository repository;

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private EntityManager entityManager;

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + DATABASE);
    }

    @Test
    void persistsPendingBillUsingExpectedAmountAsEffectiveAmount() {
        var data = billData();
        var bill = repository.saveAndFlush(bill("Luz", data, 2026, 9, 25000L, LocalDate.of(2026, 9, 15)));
        entityManager.clear();

        var persisted = repository.findById(bill.getId()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(HouseholdBillStatus.PENDING);
        assertThat(persisted.getExpectedAmountCents()).isEqualTo(25000L);
        assertThat(persisted.getActualAmountCents()).isNull();
        assertThat(persisted.getEffectiveAmountCents()).isEqualTo(25000L);
        assertThat(persisted.getResponsiblePerson().getId()).isEqualTo(data.person().getId());
        assertThat(persisted.getCategory().getId()).isEqualTo(data.category().getId());
        assertThat(persisted.getCreatedAt()).isNotNull();
    }

    @Test
    void actualAmountOverridesEstimateAndAmountsCanBeUpdated() {
        var data = billData();
        var bill = repository.saveAndFlush(bill("Luz", data, 2026, 9, 25000L, LocalDate.of(2026, 9, 15)));

        bill.updateExpectedAmount(26000L);
        bill.setActualAmount(28743L);
        repository.flush();
        entityManager.clear();

        var updated = repository.findById(bill.getId()).orElseThrow();
        assertThat(updated.getExpectedAmountCents()).isEqualTo(26000L);
        assertThat(updated.getActualAmountCents()).isEqualTo(28743L);
        assertThat(updated.getEffectiveAmountCents()).isEqualTo(28743L);
    }

    @Test
    void persistsPartialPaidAndCancelledStates() {
        var data = billData();
        var bill = repository.saveAndFlush(bill("Internet", data, 2026, 9, 10000L, LocalDate.of(2026, 9, 10)));

        bill.markPartiallyPaid();
        repository.flush();
        assertThat(bill.getStatus()).isEqualTo(HouseholdBillStatus.PARTIALLY_PAID);

        bill.markPaid();
        repository.flush();
        assertThat(bill.getStatus()).isEqualTo(HouseholdBillStatus.PAID);

        bill.cancel();
        repository.flush();
        assertThat(bill.getStatus()).isEqualTo(HouseholdBillStatus.CANCELLED);
        assertThatThrownBy(bill::markPaid).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsInvalidReferenceMonthAndNegativeAmounts() {
        var person = new Person("Mãe", PersonType.USER);
        var category = new Category("Casa", CategoryType.EXPENSE);

        assertThatThrownBy(() -> new HouseholdBill(
                "Luz", category, person, 2026, 13, 0L, LocalDate.of(2026, 9, 1)
        )).isInstanceOf(IllegalArgumentException.class).hasMessage("referenceMonth must be between 1 and 12");
        assertThatThrownBy(() -> new HouseholdBill(
                "Luz", category, person, 2026, 9, -1L, LocalDate.of(2026, 9, 1)
        )).isInstanceOf(IllegalArgumentException.class).hasMessage("expectedAmountCents cannot be negative");
        var bill = new HouseholdBill("Luz", category, person, 2026, 9, 0L, LocalDate.of(2026, 9, 1));
        assertThatThrownBy(() -> bill.setActualAmount(-1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("actualAmountCents cannot be negative");
    }

    @Test
    void rejectsIncomeCategoryAndAcceptsExpenseCategory() {
        var person = new Person("Mãe", PersonType.USER);
        var incomeCategory = new Category("Salário", CategoryType.INCOME);
        var expenseCategory = new Category("Casa", CategoryType.EXPENSE);

        assertThatThrownBy(() -> new HouseholdBill(
                "Luz", incomeCategory, person, 2026, 9, 100L, LocalDate.of(2026, 9, 15)
        )).isInstanceOf(IllegalArgumentException.class).hasMessage("category must have type EXPENSE");

        assertThat(new HouseholdBill(
                "Luz", expenseCategory, person, 2026, 9, 100L, LocalDate.of(2026, 9, 15)
        ).getCategory()).isSameAs(expenseCategory);
    }

    @Test
    void queriesByReferenceStatusDueDateAndResponsiblePerson() {
        var mother = personRepository.save(new Person("Mãe", PersonType.USER));
        var saulo = personRepository.save(new Person("Saulo", PersonType.USER));
        var category = categoryRepository.saveAndFlush(new Category("Casa", CategoryType.EXPENSE));
        var dataMother = new BillData(mother, category);
        var dataSaulo = new BillData(saulo, category);
        var august = repository.save(bill("Água", dataMother, 2026, 8, 100L, LocalDate.of(2026, 8, 10)));
        var september = repository.save(bill("Luz", dataMother, 2026, 9, 100L, LocalDate.of(2026, 9, 15)));
        september.markPartiallyPaid();
        var october = repository.saveAndFlush(bill("Internet", dataSaulo, 2026, 10, 100L, LocalDate.of(2026, 10, 10)));

        assertThat(repository.findAllByReferenceYearAndReferenceMonth(2026, 9)).containsExactly(september);
        assertThat(repository.findAllByStatus(HouseholdBillStatus.PARTIALLY_PAID)).containsExactly(september);
        assertThat(repository.findAllByDueDateBetween(
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)
        )).containsExactly(september).doesNotContain(august, october);
        assertThat(repository.findAllByResponsiblePerson(mother)).containsExactlyInAnyOrder(august, september);
    }

    private BillData billData() {
        var person = personRepository.save(new Person("Mãe", PersonType.USER));
        var category = categoryRepository.saveAndFlush(new Category("Casa", CategoryType.EXPENSE));
        return new BillData(person, category);
    }

    private static HouseholdBill bill(
            String name,
            BillData data,
            int year,
            int month,
            long amount,
            LocalDate dueDate
    ) {
        return new HouseholdBill(name, data.category(), data.person(), year, month, amount, dueDate);
    }

    private record BillData(Person person, Category category) {
    }

    private static Path createTemporaryDatabase() {
        try {
            var database = Files.createTempFile("ninho-household-bill-repository-", ".db");
            database.toFile().deleteOnExit();
            return database;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not create the test database", exception);
        }
    }
}
