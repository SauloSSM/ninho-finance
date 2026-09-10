package br.com.saulossm.ninho;

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
import br.com.saulossm.ninho.expense.ExpenseAllocation;
import br.com.saulossm.ninho.expense.ExpenseAllocationRepository;
import br.com.saulossm.ninho.expense.ExpenseRepository;
import br.com.saulossm.ninho.expense.ExpenseScope;
import br.com.saulossm.ninho.income.IncomeRepository;
import br.com.saulossm.ninho.payment.PaymentRepository;
import br.com.saulossm.ninho.person.Person;
import br.com.saulossm.ninho.person.PersonRepository;
import br.com.saulossm.ninho.person.PersonType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AlphaApiIntegrationTest {

    private static final Path DATABASE = createTemporaryDatabase();
    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private HouseholdBillRepository billRepository;

    @Autowired
    private CreditCardRepository cardRepository;

    @Autowired
    private CreditCardInvoiceRepository invoiceRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private ExpenseAllocationRepository allocationRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private IncomeRepository incomeRepository;

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + DATABASE);
    }

    @Test
    void createsExpenseWithAllocationsAtomically() throws Exception {
        var first = person("User A");
        var second = person("User B");
        var category = expenseCategory();
        long expensesBefore = expenseRepository.count();
        long allocationsBefore = allocationRepository.count();

        mockMvc.perform(post("/api/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(expenseJson(category.getId(), 30000, first.getId(), 18000, second.getId(), 12000)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amountCents").value(30000))
                .andExpect(jsonPath("$.allocations.length()").value(2));

        assertThat(expenseRepository.count()).isEqualTo(expensesBefore + 1);
        assertThat(allocationRepository.count()).isEqualTo(allocationsBefore + 2);
    }

    @Test
    void rollsBackExpenseWhenAnAllocationReferencesMissingPerson() throws Exception {
        var first = person("Valid user");
        var category = expenseCategory();
        long expensesBefore = expenseRepository.count();
        long allocationsBefore = allocationRepository.count();

        mockMvc.perform(post("/api/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(expenseJson(category.getId(), 30000, first.getId(), 15000, Long.MAX_VALUE, 15000)))
                .andExpect(status().isNotFound());

        assertThat(expenseRepository.count()).isEqualTo(expensesBefore);
        assertThat(allocationRepository.count()).isEqualTo(allocationsBefore);
    }

    @Test
    void rejectsExpenseWhenAllocationSumDiffers() throws Exception {
        var first = person("User sum");
        var category = expenseCategory();
        long before = expenseRepository.count();

        mockMvc.perform(post("/api/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(expenseJson(category.getId(), 30000, first.getId(), 29000)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("allocation total must equal expense amount"));

        assertThat(expenseRepository.count()).isEqualTo(before);
    }

    @Test
    void rejectsDuplicatePersonAllocation() throws Exception {
        var first = person("User duplicate");
        var category = expenseCategory();

        mockMvc.perform(post("/api/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(expenseJson(category.getId(), 30000, first.getId(), 15000, first.getId(), 15000)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("a person cannot appear more than once in allocations"));
    }

    @Test
    void partialHouseholdBillPaymentUpdatesStatus() throws Exception {
        var payer = person("Partial payer");
        var bill = bill(payer, 10000, 2026, 9);

        postBillPayment(payer.getId(), bill.getId(), 4000).andExpect(status().isCreated());

        mockMvc.perform(get("/api/bills/{id}", bill.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PARTIALLY_PAID"))
                .andExpect(jsonPath("$.paidAmountCents").value(4000))
                .andExpect(jsonPath("$.outstandingAmountCents").value(6000));
    }

    @Test
    void finalHouseholdBillPaymentMarksItPaid() throws Exception {
        var payer = person("Final payer");
        var bill = bill(payer, 10000, 2026, 9);

        postBillPayment(payer.getId(), bill.getId(), 4000).andExpect(status().isCreated());
        postBillPayment(payer.getId(), bill.getId(), 6000).andExpect(status().isCreated());

        mockMvc.perform(get("/api/bills/{id}", bill.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.outstandingAmountCents").value(0));
    }

    @Test
    void rejectsPaymentAboveTargetAmount() throws Exception {
        var payer = person("Over payer");
        var bill = bill(payer, 10000, 2026, 9);
        long before = paymentRepository.count();

        postBillPayment(payer.getId(), bill.getId(), 10001)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Household bill payment exceeds outstanding amount"));

        assertThat(paymentRepository.count()).isEqualTo(before);
    }

    @Test
    void rejectsPaymentWithZeroTargets() throws Exception {
        var payer = person("No target payer");

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paymentJson(payer.getId(), 100, null, null, null)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsPaymentWithMultipleTargets() throws Exception {
        var payer = person("Multi target payer");
        var bill = bill(payer, 10000, 2026, 9);
        var expense = expense(payer, null, 10000, ExpenseScope.PERSONAL, LocalDateTime.of(2026, 9, 10, 12, 0));

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paymentJson(payer.getId(), 100, expense.getId(), bill.getId(), null)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void calculatesInvoiceTotalAndReconciliation() throws Exception {
        var owner = person("Invoice owner");
        var invoice = invoice(owner, 2026, 9, 12500L);
        expense(owner, invoice, 12500, ExpenseScope.HOUSEHOLD, LocalDateTime.of(2026, 9, 4, 10, 0));

        mockMvc.perform(get("/api/invoices/{id}", invoice.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.calculatedTotalCents").value(12500))
                .andExpect(jsonPath("$.paidAmountCents").value(0))
                .andExpect(jsonPath("$.outstandingAmountCents").value(12500))
                .andExpect(jsonPath("$.reconciled").value(true));
    }

    @Test
    void calculatesPartialInvoicePayment() throws Exception {
        var owner = person("Invoice payment owner");
        var invoice = invoice(owner, 2026, 9, null);
        expense(owner, invoice, 12000, ExpenseScope.PERSONAL, LocalDateTime.of(2026, 9, 5, 10, 0));

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paymentJson(owner.getId(), 5000, null, null, invoice.getId())))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/invoices/{id}", invoice.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paidAmountCents").value(5000))
                .andExpect(jsonPath("$.outstandingAmountCents").value(7000))
                .andExpect(jsonPath("$.reconciled").doesNotExist());
    }

    @Test
    void reportsInvoiceReconciliationMismatch() throws Exception {
        var owner = person("Mismatch owner");
        var invoice = invoice(owner, 2026, 8, 9000L);
        expense(owner, invoice, 8000, ExpenseScope.PERSONAL, LocalDateTime.of(2026, 8, 5, 10, 0));

        mockMvc.perform(get("/api/invoices/{id}", invoice.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reconciled").value(false));
    }

    @Test
    void receivesExpectedIncome() throws Exception {
        var person = person("Income receiver");
        var category = incomeCategory();

        var result = mockMvc.perform(post("/api/incomes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"personId":%d,"categoryId":%d,"description":"Test income",\
                                "amountCents":50000,"expectedDate":"2026-09-05"}
                                """.formatted(person.getId(), category.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("EXPECTED"))
                .andReturn();
        long incomeId = extractId(result.getResponse().getContentAsString());

        mockMvc.perform(patch("/api/incomes/{id}/receive", incomeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"receivedAt\":\"2026-09-05T09:30:00\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECEIVED"))
                .andExpect(jsonPath("$.receivedAt").value("2026-09-05T09:30:00"));

        assertThat(incomeRepository.findById(incomeId).orElseThrow().getStatus().name()).isEqualTo("RECEIVED");
    }

    @Test
    void monthlyDetailEndpointsFilterAndSortTheirPeriods() throws Exception {
        var owner = person("Monthly owner");
        var februaryLater = bill(owner, 1000, 2025, 2, LocalDate.of(2025, 2, 20));
        var februaryEarlier = bill(owner, 2000, 2025, 2, LocalDate.of(2025, 2, 5));
        bill(owner, 3000, 2025, 3);
        var februaryInvoice = invoice(owner, 2025, 2, null);
        invoice(owner, 2025, 3, null);

        mockMvc.perform(get("/api/overview/monthly/bills").param("year", "2025").param("month", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(februaryEarlier.getId()))
                .andExpect(jsonPath("$[1].id").value(februaryLater.getId()));

        mockMvc.perform(get("/api/overview/monthly/invoices").param("year", "2025").param("month", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(februaryInvoice.getId()));
    }

    @Test
    void monthlyOverviewAvoidsDoubleCountingCardExpenses() throws Exception {
        var owner = person("Overview owner");
        var invoice = invoice(owner, 2026, 7, null);
        expense(owner, invoice, 10000, ExpenseScope.HOUSEHOLD, LocalDateTime.of(2026, 7, 4, 10, 0));
        expense(owner, null, 2000, ExpenseScope.PERSONAL, LocalDateTime.of(2026, 7, 5, 10, 0));
        expense(owner, null, 3000, ExpenseScope.HOUSEHOLD, LocalDateTime.of(2026, 7, 6, 10, 0));
        bill(owner, 4000, 2026, 7);

        mockMvc.perform(get("/api/overview/monthly").param("year", "2026").param("month", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.creditCards.totalCents").value(10000))
                .andExpect(jsonPath("$.householdBills.totalCents").value(4000))
                .andExpect(jsonPath("$.expenses.personalCents").value(2000))
                .andExpect(jsonPath("$.expenses.householdCents").value(3000))
                .andExpect(jsonPath("$.expenses.totalCents").value(5000));
    }

    @Test
    void returns400ForInvalidRequest() throws Exception {
        mockMvc.perform(post("/api/people")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\" \",\"type\":\"USER\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"));
    }

    @Test
    void returns404ForMissingId() throws Exception {
        mockMvc.perform(get("/api/people/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource not found"));
    }

    @Test
    void returns409ForRelevantUniquenessConflict() throws Exception {
        String name = unique("Unique category");
        String body = "{\"name\":\"%s\",\"type\":\"EXPENSE\"}".formatted(name);

        mockMvc.perform(post("/api/categories").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/categories").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflict"));
    }

    private org.springframework.test.web.servlet.ResultActions postBillPayment(long payerId, long billId, long amount)
            throws Exception {
        return mockMvc.perform(post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(paymentJson(payerId, amount, null, billId, null)));
    }

    private Person person(String prefix) {
        return personRepository.saveAndFlush(new Person(unique(prefix), PersonType.USER));
    }

    private Category expenseCategory() {
        return categoryRepository.saveAndFlush(new Category(unique("Expense category"), CategoryType.EXPENSE));
    }

    private Category incomeCategory() {
        return categoryRepository.saveAndFlush(new Category(unique("Income category"), CategoryType.INCOME));
    }

    private HouseholdBill bill(Person responsible, long amount, int year, int month) {
        return bill(responsible, amount, year, month, LocalDate.of(year, month, 15));
    }

    private HouseholdBill bill(Person responsible, long amount, int year, int month, LocalDate dueDate) {
        return billRepository.saveAndFlush(new HouseholdBill(
                unique("Bill"), expenseCategory(), responsible, year, month, amount, dueDate
        ));
    }

    private CreditCardInvoice invoice(Person owner, int year, int month, Long reportedTotal) {
        var card = cardRepository.saveAndFlush(new CreditCard(
                unique("Card"), "Test Bank", owner, owner, owner, "1234", 100000L, 5, 12
        ));
        return invoiceRepository.saveAndFlush(new CreditCardInvoice(
                card, year, month, LocalDate.of(year, month, 12), reportedTotal
        ));
    }

    private Expense expense(
            Person person,
            CreditCardInvoice invoice,
            long amount,
            ExpenseScope scope,
            LocalDateTime occurredAt
    ) {
        var expense = expenseRepository.saveAndFlush(new Expense(
                unique("Expense"), expenseCategory(), amount, scope, occurredAt, invoice, null
        ));
        allocationRepository.saveAndFlush(new ExpenseAllocation(expense, person, amount));
        return expense;
    }

    private static String expenseJson(long categoryId, long amount, Object... allocations) {
        var allocationJson = new StringBuilder();
        for (int index = 0; index < allocations.length; index += 2) {
            if (!allocationJson.isEmpty()) {
                allocationJson.append(',');
            }
            allocationJson.append("{\"personId\":")
                    .append(allocations[index])
                    .append(",\"amountCents\":")
                    .append(allocations[index + 1])
                    .append('}');
        }
        return """
                {"description":"Test expense","categoryId":%d,"amountCents":%d,"scope":"HOUSEHOLD",\
                "occurredAt":"2026-09-10T12:00:00","allocations":[%s]}
                """.formatted(categoryId, amount, allocationJson);
    }

    private static String paymentJson(
            long payerId,
            long amount,
            Long expenseId,
            Long billId,
            Long invoiceId
    ) {
        return """
                {"payerPersonId":%d,"method":"PIX","amountCents":%d,"paidAt":"2026-09-10T12:00:00",\
                "expenseId":%s,"householdBillId":%s,"creditCardInvoiceId":%s}
                """.formatted(payerId, amount, expenseId, billId, invoiceId);
    }

    private static long extractId(String json) {
        int start = json.indexOf(":") + 1;
        int end = json.indexOf(",", start);
        return Long.parseLong(json.substring(start, end));
    }

    private static String unique(String prefix) {
        return prefix + " " + SEQUENCE.incrementAndGet();
    }

    private static Path createTemporaryDatabase() {
        try {
            var database = Files.createTempFile("ninho-alpha-api-", ".db");
            database.toFile().deleteOnExit();
            return database;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not create the test database", exception);
        }
    }
}
