package br.com.saulossm.ninho.payment;

import br.com.saulossm.ninho.account.BankAccount;
import br.com.saulossm.ninho.bill.HouseholdBill;
import br.com.saulossm.ninho.card.CreditCardInvoice;
import br.com.saulossm.ninho.expense.Expense;
import br.com.saulossm.ninho.persistence.LocalDateTimeStringConverter;
import br.com.saulossm.ninho.person.Person;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.Generated;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "INTEGER")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payer_person_id", nullable = false)
    private Person payerPerson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_account_id")
    private BankAccount bankAccount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod method;

    @Column(name = "amount_cents", nullable = false, columnDefinition = "INTEGER")
    private Long amountCents;

    @Convert(converter = LocalDateTimeStringConverter.class)
    @Column(name = "paid_at", nullable = false, columnDefinition = "TEXT")
    private LocalDateTime paidAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_id")
    private Expense expense;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "household_bill_id")
    private HouseholdBill householdBill;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credit_card_invoice_id")
    private CreditCardInvoice creditCardInvoice;

    @Column
    private String notes;

    @Generated
    @Column(
            name = "created_at",
            nullable = false,
            insertable = false,
            updatable = false,
            columnDefinition = "TEXT"
    )
    private LocalDateTime createdAt;

    protected Payment() {
    }

    public Payment(
            Person payerPerson,
            BankAccount bankAccount,
            PaymentMethod method,
            Long amountCents,
            LocalDateTime paidAt,
            Expense expense,
            HouseholdBill householdBill,
            CreditCardInvoice creditCardInvoice,
            String notes
    ) {
        this.payerPerson = Objects.requireNonNull(payerPerson, "payerPerson is required");
        this.bankAccount = bankAccount;
        this.method = Objects.requireNonNull(method, "method is required");
        this.amountCents = requirePositive(amountCents);
        this.paidAt = Objects.requireNonNull(paidAt, "paidAt is required");
        requireExactlyOneTarget(expense, householdBill, creditCardInvoice);
        this.expense = expense;
        this.householdBill = householdBill;
        this.creditCardInvoice = creditCardInvoice;
        this.notes = notes;
    }

    public Long getId() {
        return id;
    }

    public Person getPayerPerson() {
        return payerPerson;
    }

    public BankAccount getBankAccount() {
        return bankAccount;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public Long getAmountCents() {
        return amountCents;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public Expense getExpense() {
        return expense;
    }

    public HouseholdBill getHouseholdBill() {
        return householdBill;
    }

    public CreditCardInvoice getCreditCardInvoice() {
        return creditCardInvoice;
    }

    public String getNotes() {
        return notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    private static Long requirePositive(Long amountCents) {
        Objects.requireNonNull(amountCents, "amountCents is required");
        if (amountCents <= 0) {
            throw new IllegalArgumentException("amountCents must be positive");
        }
        return amountCents;
    }

    private static void requireExactlyOneTarget(
            Expense expense,
            HouseholdBill householdBill,
            CreditCardInvoice creditCardInvoice
    ) {
        int targets = 0;
        targets += expense == null ? 0 : 1;
        targets += householdBill == null ? 0 : 1;
        targets += creditCardInvoice == null ? 0 : 1;
        if (targets != 1) {
            throw new IllegalArgumentException("payment must have exactly one target");
        }
    }
}
