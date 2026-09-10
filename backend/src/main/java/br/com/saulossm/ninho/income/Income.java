package br.com.saulossm.ninho.income;

import br.com.saulossm.ninho.account.BankAccount;
import br.com.saulossm.ninho.category.Category;
import br.com.saulossm.ninho.category.CategoryType;
import br.com.saulossm.ninho.persistence.LocalDateStringConverter;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "incomes")
public class Income {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "INTEGER")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_account_id")
    private BankAccount bankAccount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false)
    private String description;

    @Column(name = "amount_cents", nullable = false, columnDefinition = "INTEGER")
    private Long amountCents;

    @Convert(converter = LocalDateStringConverter.class)
    @Column(name = "expected_date", nullable = false, columnDefinition = "TEXT")
    private LocalDate expectedDate;

    @Convert(converter = LocalDateTimeStringConverter.class)
    @Column(name = "received_at", columnDefinition = "TEXT")
    private LocalDateTime receivedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncomeStatus status = IncomeStatus.EXPECTED;

    @Generated
    @Column(
            name = "created_at",
            nullable = false,
            insertable = false,
            updatable = false,
            columnDefinition = "TEXT"
    )
    private LocalDateTime createdAt;

    protected Income() {
    }

    public Income(
            Person person,
            BankAccount bankAccount,
            Category category,
            String description,
            Long amountCents,
            LocalDate expectedDate
    ) {
        this.person = Objects.requireNonNull(person, "person is required");
        this.bankAccount = bankAccount;
        this.category = requireIncomeCategory(category);
        this.description = requireText(description, "description is required");
        this.amountCents = requirePositive(amountCents, "amountCents");
        this.expectedDate = Objects.requireNonNull(expectedDate, "expectedDate is required");
    }

    public Long getId() {
        return id;
    }

    public Person getPerson() {
        return person;
    }

    public BankAccount getBankAccount() {
        return bankAccount;
    }

    public Category getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public Long getAmountCents() {
        return amountCents;
    }

    public LocalDate getExpectedDate() {
        return expectedDate;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public IncomeStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void markReceived(LocalDateTime receivedAt) {
        markReceived(receivedAt, bankAccount);
    }

    public void markReceived(LocalDateTime receivedAt, BankAccount bankAccount) {
        if (status != IncomeStatus.EXPECTED) {
            throw new IllegalStateException("only expected income can be received");
        }
        this.receivedAt = Objects.requireNonNull(receivedAt, "receivedAt is required");
        this.bankAccount = bankAccount;
        status = IncomeStatus.RECEIVED;
    }

    public void cancel() {
        if (status != IncomeStatus.EXPECTED) {
            throw new IllegalStateException("only expected income can be cancelled");
        }
        status = IncomeStatus.CANCELLED;
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private static Category requireIncomeCategory(Category category) {
        Objects.requireNonNull(category, "category is required");
        if (category.getType() != CategoryType.INCOME) {
            throw new IllegalArgumentException("category must have type INCOME");
        }
        return category;
    }

    private static Long requirePositive(Long value, String field) {
        Objects.requireNonNull(value, field + " is required");
        if (value <= 0) {
            throw new IllegalArgumentException(field + " must be positive");
        }
        return value;
    }
}
