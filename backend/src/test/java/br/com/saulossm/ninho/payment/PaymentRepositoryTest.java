package br.com.saulossm.ninho.payment;

import br.com.saulossm.ninho.account.BankAccount;
import br.com.saulossm.ninho.account.BankAccountRepository;
import br.com.saulossm.ninho.bill.HouseholdBill;
import br.com.saulossm.ninho.bill.HouseholdBillRepository;
import br.com.saulossm.ninho.card.CreditCard;
import br.com.saulossm.ninho.card.CreditCardInvoice;
import br.com.saulossm.ninho.card.CreditCardInvoiceRepository;
import br.com.saulossm.ninho.card.CreditCardRepository;
import br.com.saulossm.ninho.category.Category;
import br.com.saulossm.ninho.category.CategoryRepository;
import br.com.saulossm.ninho.category.CategoryType;
import br.com.saulossm.ninho.expense.Expense;
import br.com.saulossm.ninho.expense.ExpenseRepository;
import br.com.saulossm.ninho.expense.ExpenseScope;
import br.com.saulossm.ninho.person.Person;
import br.com.saulossm.ninho.person.PersonRepository;
import br.com.saulossm.ninho.person.PersonType;
import jakarta.persistence.EntityManager;
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
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PaymentRepositoryTest {

    private static final Path DATABASE = createTemporaryDatabase();

    @Autowired
    private PaymentRepository repository;

    @Autowired
    private SettlementPaymentRepository settlementRepository;

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private BankAccountRepository accountRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private HouseholdBillRepository billRepository;

    @Autowired
    private CreditCardRepository cardRepository;

    @Autowired
    private CreditCardInvoiceRepository invoiceRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + DATABASE);
    }

    @Test
    void persistsExpensePaymentAsCashMovementWithoutCreatingAnotherExpense() {
        var data = paymentData();
        long expenseCount = expenseRepository.count();
        var paidAt = LocalDateTime.of(2026, 9, 10, 12, 0);
        var payment = repository.saveAndFlush(new Payment(
                data.payer(), data.account(), PaymentMethod.PIX, 30000L, paidAt,
                data.expense(), null, null, "Pagamento do mercado"
        ));
        entityManager.clear();

        var persisted = repository.findById(payment.getId()).orElseThrow();
        assertThat(persisted.getExpense().getId()).isEqualTo(data.expense().getId());
        assertThat(persisted.getHouseholdBill()).isNull();
        assertThat(persisted.getCreditCardInvoice()).isNull();
        assertThat(persisted.getPayerPerson().getId()).isEqualTo(data.payer().getId());
        assertThat(persisted.getBankAccount().getId()).isEqualTo(data.account().getId());
        assertThat(persisted.getMethod()).isEqualTo(PaymentMethod.PIX);
        assertThat(persisted.getPaidAt()).isEqualTo(paidAt);
        assertThat(persisted.getCreatedAt()).isNotNull();
        assertThat(expenseRepository.count()).isEqualTo(expenseCount);
    }

    @Test
    void persistsBillAndInvoicePaymentsAsDistinctTargets() {
        var data = paymentData();
        var billPayment = repository.save(new Payment(
                data.payer(), data.account(), PaymentMethod.BOLETO, 10000L, LocalDateTime.now(),
                null, data.bill(), null, null
        ));
        var invoicePayment = repository.saveAndFlush(new Payment(
                data.payer(), data.account(), PaymentMethod.TRANSFER, 20000L, LocalDateTime.now(),
                null, null, data.invoice(), null
        ));

        assertThat(repository.findAllByHouseholdBill(data.bill())).containsExactly(billPayment);
        assertThat(repository.findAllByCreditCardInvoice(data.invoice())).containsExactly(invoicePayment);
        assertThat(repository.findAllByExpense(data.expense())).isEmpty();
    }

    @Test
    void rejectsPaymentsWithZeroOrMultipleTargetsInDomain() {
        var data = paymentData();

        assertThatThrownBy(() -> new Payment(
                data.payer(), data.account(), PaymentMethod.CASH, 100L, LocalDateTime.now(),
                null, null, null, null
        )).isInstanceOf(IllegalArgumentException.class).hasMessage("payment must have exactly one target");
        assertThatThrownBy(() -> new Payment(
                data.payer(), data.account(), PaymentMethod.CASH, 100L, LocalDateTime.now(),
                data.expense(), data.bill(), null, null
        )).isInstanceOf(IllegalArgumentException.class).hasMessage("payment must have exactly one target");
    }

    @Test
    void sqliteRejectsPaymentsWithZeroOrMultipleTargets() {
        var data = paymentData();
        var sql = """
                INSERT INTO payments
                    (payer_person_id, method, amount_cents, paid_at, expense_id, household_bill_id)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        assertThatThrownBy(() -> jdbcTemplate.update(
                sql, data.payer().getId(), "PIX", 100L, "2026-09-10 12:00:00", null, null
        )).isInstanceOf(DataAccessException.class).hasMessageContaining("CHECK constraint failed");
        assertThatThrownBy(() -> jdbcTemplate.update(
                sql, data.payer().getId(), "PIX", 100L, "2026-09-10 12:00:00",
                data.expense().getId(), data.bill().getId()
        )).isInstanceOf(DataAccessException.class).hasMessageContaining("CHECK constraint failed");
    }

    @Test
    void rejectsNonPositivePaymentAmount() {
        var data = paymentData();
        assertThatThrownBy(() -> new Payment(
                data.payer(), null, PaymentMethod.OTHER, 0L, LocalDateTime.now(),
                data.expense(), null, null, null
        )).isInstanceOf(IllegalArgumentException.class).hasMessage("amountCents must be positive");
    }

    @Test
    void representsPartialPaymentWithMultiplePaymentRows() {
        var data = paymentData();
        var first = repository.save(new Payment(
                data.payer(), data.account(), PaymentMethod.PIX, 10000L, LocalDateTime.now(),
                null, data.bill(), null, null
        ));
        var second = repository.saveAndFlush(new Payment(
                data.payer(), data.account(), PaymentMethod.PIX, 5000L, LocalDateTime.now(),
                null, data.bill(), null, null
        ));

        assertThat(repository.findAllByHouseholdBill(data.bill())).containsExactlyInAnyOrder(first, second);
        assertThat(repository.findAllByHouseholdBill(data.bill()))
                .extracting(Payment::getAmountCents)
                .containsExactlyInAnyOrder(10000L, 5000L);
    }

    @Test
    void persistsSettlementAndQueriesSentReceivedAndBetweenPeople() {
        var mother = personRepository.save(new Person("Mãe", PersonType.USER));
        var saulo = personRepository.save(new Person("Saulo", PersonType.USER));
        var other = personRepository.saveAndFlush(new Person("Madrinha", PersonType.EXTERNAL));
        var motherToSaulo = settlementRepository.save(new SettlementPayment(
                mother, saulo, 30000L, LocalDateTime.of(2026, 9, 10, 10, 0), "Acerto Nubank"
        ));
        var sauloToOther = settlementRepository.saveAndFlush(new SettlementPayment(
                saulo, other, 5000L, LocalDateTime.of(2026, 9, 10, 11, 0), null
        ));

        assertThat(settlementRepository.findAllByFromPerson(mother)).containsExactly(motherToSaulo);
        assertThat(settlementRepository.findAllByToPerson(saulo)).containsExactly(motherToSaulo);
        assertThat(settlementRepository.findAllByFromPersonAndToPerson(mother, saulo))
                .containsExactly(motherToSaulo);
        assertThat(settlementRepository.findAllByFromPerson(saulo)).containsExactly(sauloToOther);
        assertThat(motherToSaulo.getCreatedAt()).isNotNull();
    }

    @Test
    void rejectsSelfSettlementAndNonPositiveAmount() {
        var mother = new Person("Mãe", PersonType.USER);
        var saulo = new Person("Saulo", PersonType.USER);

        assertThatThrownBy(() -> new SettlementPayment(mother, mother, 100L, LocalDateTime.now(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("fromPerson and toPerson must be different");
        assertThatThrownBy(() -> new SettlementPayment(mother, saulo, 0L, LocalDateTime.now(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("amountCents must be positive");
    }

    private PaymentData paymentData() {
        var payer = personRepository.save(new Person("Mãe", PersonType.USER));
        var category = categoryRepository.save(new Category("Casa", CategoryType.EXPENSE));
        var account = accountRepository.save(new BankAccount("Caixa", "Caixa", payer, 0L));
        var card = cardRepository.save(new CreditCard(
                "Itaú", "Itaú", payer, payer, payer, "1234", 500000L, 5, 12
        ));
        var invoice = invoiceRepository.save(new CreditCardInvoice(
                card, 2026, 9, LocalDate.of(2026, 9, 12), null
        ));
        var expense = expenseRepository.save(new Expense(
                "Mercado", category, 30000L, ExpenseScope.HOUSEHOLD, LocalDateTime.now(), invoice, null
        ));
        var bill = billRepository.saveAndFlush(new HouseholdBill(
                "Luz", category, payer, 2026, 9, 25000L, LocalDate.of(2026, 9, 15)
        ));
        return new PaymentData(payer, account, expense, bill, invoice);
    }

    private record PaymentData(
            Person payer,
            BankAccount account,
            Expense expense,
            HouseholdBill bill,
            CreditCardInvoice invoice
    ) {
    }

    private static Path createTemporaryDatabase() {
        try {
            var database = Files.createTempFile("ninho-payment-repository-", ".db");
            database.toFile().deleteOnExit();
            return database;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not create the test database", exception);
        }
    }
}
