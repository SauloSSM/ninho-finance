package br.com.saulossm.ninho.card;

import br.com.saulossm.ninho.persistence.LocalDateStringConverter;
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
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.Generated;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(
        name = "credit_card_invoices",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_credit_card_invoices_card_reference",
                columnNames = {"credit_card_id", "reference_year", "reference_month"}
        )
)
public class CreditCardInvoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "INTEGER")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "credit_card_id", nullable = false)
    private CreditCard creditCard;

    @Column(name = "reference_year", nullable = false, columnDefinition = "INTEGER")
    private Integer referenceYear;

    @Column(name = "reference_month", nullable = false, columnDefinition = "INTEGER")
    private Integer referenceMonth;

    @Convert(converter = LocalDateStringConverter.class)
    @Column(name = "due_date", nullable = false, columnDefinition = "TEXT")
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceStatus status = InvoiceStatus.OPEN;

    @Column(name = "reported_total_cents", columnDefinition = "INTEGER")
    private Long reportedTotalCents;

    @Generated
    @Column(
            name = "created_at",
            nullable = false,
            insertable = false,
            updatable = false,
            columnDefinition = "TEXT"
    )
    private LocalDateTime createdAt;

    protected CreditCardInvoice() {
    }

    public CreditCardInvoice(
            CreditCard creditCard,
            Integer referenceYear,
            Integer referenceMonth,
            LocalDate dueDate,
            Long reportedTotalCents
    ) {
        this.creditCard = Objects.requireNonNull(creditCard, "creditCard is required");
        this.referenceYear = requirePositiveYear(referenceYear);
        this.referenceMonth = requireMonth(referenceMonth);
        this.dueDate = Objects.requireNonNull(dueDate, "dueDate is required");
        this.reportedTotalCents = requireOptionalNonNegative(reportedTotalCents);
    }

    public Long getId() {
        return id;
    }

    public CreditCard getCreditCard() {
        return creditCard;
    }

    public Integer getReferenceYear() {
        return referenceYear;
    }

    public Integer getReferenceMonth() {
        return referenceMonth;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public InvoiceStatus getStatus() {
        return status;
    }

    public Long getReportedTotalCents() {
        return reportedTotalCents;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void close() {
        status = InvoiceStatus.CLOSED;
    }

    public void reopen() {
        status = InvoiceStatus.OPEN;
    }

    public void updateReportedTotal(Long reportedTotalCents) {
        this.reportedTotalCents = requireOptionalNonNegative(reportedTotalCents);
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

    private static Long requireOptionalNonNegative(Long value) {
        if (value != null && value < 0) {
            throw new IllegalArgumentException("reportedTotalCents cannot be negative");
        }
        return value;
    }
}
