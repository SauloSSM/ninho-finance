package br.com.saulossm.ninho.bill;

import br.com.saulossm.ninho.category.Category;
import br.com.saulossm.ninho.category.CategoryType;
import br.com.saulossm.ninho.persistence.LocalDateStringConverter;
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
@Table(name = "household_bills")
public class HouseholdBill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "INTEGER")
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "responsible_person_id", nullable = false)
    private Person responsiblePerson;

    @Column(name = "reference_year", nullable = false, columnDefinition = "INTEGER")
    private Integer referenceYear;

    @Column(name = "reference_month", nullable = false, columnDefinition = "INTEGER")
    private Integer referenceMonth;

    @Column(name = "expected_amount_cents", nullable = false, columnDefinition = "INTEGER")
    private Long expectedAmountCents;

    @Column(name = "actual_amount_cents", columnDefinition = "INTEGER")
    private Long actualAmountCents;

    @Convert(converter = LocalDateStringConverter.class)
    @Column(name = "due_date", nullable = false, columnDefinition = "TEXT")
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HouseholdBillStatus status = HouseholdBillStatus.PENDING;

    @Generated
    @Column(
            name = "created_at",
            nullable = false,
            insertable = false,
            updatable = false,
            columnDefinition = "TEXT"
    )
    private LocalDateTime createdAt;

    protected HouseholdBill() {
    }

    public HouseholdBill(
            String name,
            Category category,
            Person responsiblePerson,
            Integer referenceYear,
            Integer referenceMonth,
            Long expectedAmountCents,
            LocalDate dueDate
    ) {
        this.name = requireText(name, "name is required");
        this.category = requireExpenseCategory(category);
        this.responsiblePerson = Objects.requireNonNull(responsiblePerson, "responsiblePerson is required");
        this.referenceYear = requirePositiveYear(referenceYear);
        this.referenceMonth = requireMonth(referenceMonth);
        this.expectedAmountCents = requireNonNegative(expectedAmountCents, "expectedAmountCents");
        this.dueDate = Objects.requireNonNull(dueDate, "dueDate is required");
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Category getCategory() {
        return category;
    }

    public Person getResponsiblePerson() {
        return responsiblePerson;
    }

    public Integer getReferenceYear() {
        return referenceYear;
    }

    public Integer getReferenceMonth() {
        return referenceMonth;
    }

    public Long getExpectedAmountCents() {
        return expectedAmountCents;
    }

    public Long getActualAmountCents() {
        return actualAmountCents;
    }

    public Long getEffectiveAmountCents() {
        return actualAmountCents != null ? actualAmountCents : expectedAmountCents;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public HouseholdBillStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void updateExpectedAmount(Long expectedAmountCents) {
        this.expectedAmountCents = requireNonNegative(expectedAmountCents, "expectedAmountCents");
    }

    public void setActualAmount(Long actualAmountCents) {
        this.actualAmountCents = requireNonNegative(actualAmountCents, "actualAmountCents");
    }

    public void markPartiallyPaid() {
        ensureNotCancelled();
        status = HouseholdBillStatus.PARTIALLY_PAID;
    }

    public void markPaid() {
        ensureNotCancelled();
        status = HouseholdBillStatus.PAID;
    }

    public void cancel() {
        status = HouseholdBillStatus.CANCELLED;
    }

    private void ensureNotCancelled() {
        if (status == HouseholdBillStatus.CANCELLED) {
            throw new IllegalStateException("cancelled bill cannot be paid");
        }
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

    private static Integer requirePositiveYear(Integer year) {
        Objects.requireNonNull(year, "referenceYear is required");
        if (year < 1) {
            throw new IllegalArgumentException("referenceYear must be positive");
        }
        return year;
    }

    private static Integer requireMonth(Integer month) {
        Objects.requireNonNull(month, "referenceMonth is required");
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("referenceMonth must be between 1 and 12");
        }
        return month;
    }

    private static Long requireNonNegative(Long value, String field) {
        Objects.requireNonNull(value, field + " is required");
        if (value < 0) {
            throw new IllegalArgumentException(field + " cannot be negative");
        }
        return value;
    }
}
