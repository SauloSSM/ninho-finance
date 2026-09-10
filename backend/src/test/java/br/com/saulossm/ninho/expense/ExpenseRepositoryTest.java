package br.com.saulossm.ninho.expense;

import br.com.saulossm.ninho.card.CreditCard;
import br.com.saulossm.ninho.card.CreditCardInvoice;
import br.com.saulossm.ninho.card.CreditCardInvoiceRepository;
import br.com.saulossm.ninho.card.CreditCardRepository;
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
import org.springframework.orm.jpa.JpaSystemException;
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
class ExpenseRepositoryTest {

    private static final Path DATABASE = createTemporaryDatabase();

    @Autowired
    private ExpenseRepository repository;

    @Autowired
    private ExpenseAllocationRepository allocationRepository;

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CreditCardRepository cardRepository;

    @Autowired
    private CreditCardInvoiceRepository invoiceRepository;

    @Autowired
    private EntityManager entityManager;

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + DATABASE);
    }

    @Test
    void persistsPersonalExpenseOutsideCreditCard() {
        var category = categoryRepository.saveAndFlush(new Category("Jogos", CategoryType.EXPENSE));
        var occurredAt = LocalDateTime.of(2026, 9, 10, 14, 30);
        var expense = repository.saveAndFlush(new Expense(
                "Steam", category, 5999L, ExpenseScope.PERSONAL, occurredAt, null, "Promoção"
        ));
        entityManager.clear();

        var persisted = repository.findById(expense.getId()).orElseThrow();
        assertThat(persisted.getScope()).isEqualTo(ExpenseScope.PERSONAL);
        assertThat(persisted.getCreditCardInvoice()).isNull();
        assertThat(persisted.getAmountCents()).isEqualTo(5999L);
        assertThat(persisted.getOccurredAt()).isEqualTo(occurredAt);
        assertThat(persisted.getCreatedAt()).isNotNull();
    }

    @Test
    void persistsHouseholdExpenseLinkedToInvoice() {
        var data = expenseDataWithInvoice();
        var expense = repository.saveAndFlush(new Expense(
                "Mercado", data.category(), 30000L, ExpenseScope.HOUSEHOLD,
                LocalDateTime.of(2026, 9, 8, 18, 0), data.invoice(), null
        ));
        entityManager.clear();

        var persisted = repository.findById(expense.getId()).orElseThrow();
        assertThat(persisted.getScope()).isEqualTo(ExpenseScope.HOUSEHOLD);
        assertThat(persisted.getCreditCardInvoice().getId()).isEqualTo(data.invoice().getId());
    }

    @Test
    void rejectsNonPositiveExpenseAmount() {
        var category = new Category("Mercado", CategoryType.EXPENSE);
        assertThatThrownBy(() -> new Expense(
                "Mercado", category, 0L, ExpenseScope.HOUSEHOLD, LocalDateTime.now(), null, null
        )).isInstanceOf(IllegalArgumentException.class).hasMessage("amountCents must be positive");
    }

    @Test
    void rejectsIncomeCategoryAndAcceptsExpenseCategory() {
        var incomeCategory = new Category("Salário", CategoryType.INCOME);
        var expenseCategory = new Category("Mercado", CategoryType.EXPENSE);

        assertThatThrownBy(() -> new Expense(
                "Mercado", incomeCategory, 100L, ExpenseScope.HOUSEHOLD,
                LocalDateTime.now(), null, null
        )).isInstanceOf(IllegalArgumentException.class).hasMessage("category must have type EXPENSE");

        assertThat(new Expense(
                "Mercado", expenseCategory, 100L, ExpenseScope.HOUSEHOLD,
                LocalDateTime.now(), null, null
        ).getCategory()).isSameAs(expenseCategory);
    }

    @Test
    void calculatesSplitAllocationWithoutPersistingDerivedValues() {
        var category = new Category("Restaurante", CategoryType.EXPENSE);
        var mother = new Person("Mãe", PersonType.USER);
        var saulo = new Person("Saulo", PersonType.USER);
        var expense = new Expense(
                "Restaurante", category, 20000L, ExpenseScope.HOUSEHOLD, LocalDateTime.now(), null, null
        );

        new ExpenseAllocation(expense, mother, 12000L);
        assertThat(expense.allocatedAmountCents()).isEqualTo(12000L);
        assertThat(expense.isFullyAllocated()).isFalse();

        new ExpenseAllocation(expense, saulo, 8000L);
        assertThat(expense.allocatedAmountCents()).isEqualTo(20000L);
        assertThat(expense.isFullyAllocated()).isTrue();
    }

    @Test
    void persistsOneAllocationPerPersonAndLoadsAllocationTotal() {
        var mother = personRepository.save(new Person("Mãe", PersonType.USER));
        var saulo = personRepository.save(new Person("Saulo", PersonType.USER));
        var category = categoryRepository.saveAndFlush(new Category("Restaurante", CategoryType.EXPENSE));
        var expense = repository.saveAndFlush(new Expense(
                "Restaurante", category, 20000L, ExpenseScope.HOUSEHOLD, LocalDateTime.now(), null, null
        ));
        allocationRepository.save(new ExpenseAllocation(expense, mother, 12000L));
        allocationRepository.saveAndFlush(new ExpenseAllocation(expense, saulo, 8000L));
        entityManager.clear();

        var persistedExpense = repository.findById(expense.getId()).orElseThrow();
        assertThat(persistedExpense.allocatedAmountCents()).isEqualTo(20000L);
        assertThat(persistedExpense.isFullyAllocated()).isTrue();
        assertThat(allocationRepository.findAllByExpense(persistedExpense)).hasSize(2);
        assertThat(allocationRepository.findAllByPerson(mother)).singleElement()
                .satisfies(allocation -> assertThat(allocation.getAmountCents()).isEqualTo(12000L));
    }

    @Test
    void rejectsDuplicateAllocationForSameExpenseAndPerson() {
        var mother = personRepository.save(new Person("Mãe", PersonType.USER));
        var category = categoryRepository.saveAndFlush(new Category("Mercado", CategoryType.EXPENSE));
        var expense = repository.saveAndFlush(new Expense(
                "Mercado", category, 30000L, ExpenseScope.HOUSEHOLD, LocalDateTime.now(), null, null
        ));
        allocationRepository.saveAndFlush(new ExpenseAllocation(expense, mother, 10000L));

        assertThatThrownBy(() -> allocationRepository.saveAndFlush(new ExpenseAllocation(expense, mother, 20000L)))
                .isInstanceOf(JpaSystemException.class)
                .hasMessageContaining("UNIQUE constraint failed");
    }

    @Test
    void queriesByDateScopeInvoiceAndCategory() {
        var data = expenseDataWithInvoice();
        var cardExpense = repository.save(new Expense(
                "Mercado", data.category(), 30000L, ExpenseScope.HOUSEHOLD,
                LocalDateTime.of(2026, 9, 8, 18, 0), data.invoice(), null
        ));
        var personal = repository.saveAndFlush(new Expense(
                "Farmácia", data.category(), 5000L, ExpenseScope.PERSONAL,
                LocalDateTime.of(2026, 10, 1, 9, 0), null, null
        ));

        assertThat(repository.findAllByOccurredAtBetween(
                LocalDateTime.of(2026, 9, 1, 0, 0), LocalDateTime.of(2026, 9, 30, 23, 59)
        )).containsExactly(cardExpense);
        assertThat(repository.findAllByScope(ExpenseScope.PERSONAL)).containsExactly(personal);
        assertThat(repository.findAllByCreditCardInvoice(data.invoice())).containsExactly(cardExpense);
        assertThat(repository.findAllByCategory(data.category())).containsExactlyInAnyOrder(cardExpense, personal);
    }

    private ExpenseData expenseDataWithInvoice() {
        var owner = personRepository.save(new Person("Saulo", PersonType.USER));
        var category = categoryRepository.save(new Category("Mercado", CategoryType.EXPENSE));
        var card = cardRepository.save(new CreditCard(
                "Nubank", "Nubank", owner, owner, owner, "1234", 500000L, 5, 12
        ));
        var invoice = invoiceRepository.saveAndFlush(new CreditCardInvoice(
                card, 2026, 9, LocalDate.of(2026, 9, 12), null
        ));
        return new ExpenseData(category, invoice);
    }

    private record ExpenseData(Category category, CreditCardInvoice invoice) {
    }

    private static Path createTemporaryDatabase() {
        try {
            var database = Files.createTempFile("ninho-expense-repository-", ".db");
            database.toFile().deleteOnExit();
            return database;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not create the test database", exception);
        }
    }
}
