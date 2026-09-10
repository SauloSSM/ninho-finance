package br.com.saulossm.ninho.expense;

import br.com.saulossm.ninho.card.CreditCardInvoice;
import br.com.saulossm.ninho.category.Category;
import br.com.saulossm.ninho.category.CategoryType;
import br.com.saulossm.ninho.persistence.LocalDateTimeStringConverter;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.Generated;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "expenses")
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "INTEGER")
    private Long id;

    @Column(nullable = false)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "amount_cents", nullable = false, columnDefinition = "INTEGER")
    private Long amountCents;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExpenseScope scope;

    @Convert(converter = LocalDateTimeStringConverter.class)
    @Column(name = "occurred_at", nullable = false, columnDefinition = "TEXT")
    private LocalDateTime occurredAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credit_card_invoice_id")
    private CreditCardInvoice creditCardInvoice;

    @Column
    private String notes;

    @OneToMany(mappedBy = "expense", fetch = FetchType.LAZY)
    private List<ExpenseAllocation> allocations = new ArrayList<>();

    @Generated
    @Column(
            name = "created_at",
            nullable = false,
            insertable = false,
            updatable = false,
            columnDefinition = "TEXT"
    )
    private LocalDateTime createdAt;

    protected Expense() {
    }

    public Expense(
            String description,
            Category category,
            Long amountCents,
            ExpenseScope scope,
            LocalDateTime occurredAt,
            CreditCardInvoice creditCardInvoice,
            String notes
    ) {
        this.description = requireText(description, "description is required");
        this.category = requireExpenseCategory(category);
        this.amountCents = requirePositive(amountCents, "amountCents");
        this.scope = Objects.requireNonNull(scope, "scope is required");
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt is required");
        this.creditCardInvoice = creditCardInvoice;
        this.notes = notes;
    }

    public Long getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public Category getCategory() {
        return category;
    }

    public Long getAmountCents() {
        return amountCents;
    }

    public ExpenseScope getScope() {
        return scope;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
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

    public Long allocatedAmountCents() {
        return allocations.stream()
                .mapToLong(ExpenseAllocation::getAmountCents)
                .sum();
    }

    public boolean isFullyAllocated() {
        return amountCents.equals(allocatedAmountCents());
    }

    void registerAllocation(ExpenseAllocation allocation) {
        allocations.add(Objects.requireNonNull(allocation, "allocation is required"));
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private static Category requireExpenseCategory(Category category) {
        Objects.requireNonNull(category, "category is required");
        if (category.getType() != CategoryType.EXPENSE) {
            throw new IllegalArgumentException("category must have type EXPENSE");
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
