package br.com.saulossm.ninho.card;

import br.com.saulossm.ninho.person.Person;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.Generated;
import org.hibernate.type.NumericBooleanConverter;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "credit_cards")
public class CreditCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "INTEGER")
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String institution;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "holder_id", nullable = false)
    private Person holder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_payer_id", nullable = false)
    private Person invoicePayer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "default_responsible_person_id", nullable = false)
    private Person defaultResponsiblePerson;

    @Column(name = "last_four")
    private String lastFour;

    @Column(name = "credit_limit_cents", nullable = false, columnDefinition = "INTEGER")
    private Long creditLimitCents;

    @Column(name = "closing_day", nullable = false, columnDefinition = "INTEGER")
    private Integer closingDay;

    @Column(name = "due_day", nullable = false, columnDefinition = "INTEGER")
    private Integer dueDay;

    @Convert(converter = NumericBooleanConverter.class)
    @Column(nullable = false)
    private boolean active = true;

    @Generated
    @Column(
            name = "created_at",
            nullable = false,
            insertable = false,
            updatable = false,
            columnDefinition = "TEXT"
    )
    private LocalDateTime createdAt;

    protected CreditCard() {
    }

    public CreditCard(
            String name,
            String institution,
            Person holder,
            Person invoicePayer,
            Person defaultResponsiblePerson,
            String lastFour,
            Long creditLimitCents,
            Integer closingDay,
            Integer dueDay
    ) {
        this.name = requireText(name, "name is required");
        this.institution = requireText(institution, "institution is required");
        this.holder = Objects.requireNonNull(holder, "holder is required");
        this.invoicePayer = Objects.requireNonNull(invoicePayer, "invoicePayer is required");
        this.defaultResponsiblePerson = Objects.requireNonNull(
                defaultResponsiblePerson,
                "defaultResponsiblePerson is required"
        );
        this.lastFour = validateLastFour(lastFour);
        this.creditLimitCents = requireNonNegative(creditLimitCents, "creditLimitCents");
        this.closingDay = requireDay(closingDay, "closingDay");
        this.dueDay = requireDay(dueDay, "dueDay");
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getInstitution() {
        return institution;
    }

    public Person getHolder() {
        return holder;
    }

    public Person getInvoicePayer() {
        return invoicePayer;
    }

    public Person getDefaultResponsiblePerson() {
        return defaultResponsiblePerson;
    }

    public String getLastFour() {
        return lastFour;
    }

    public Long getCreditLimitCents() {
        return creditLimitCents;
    }

    public Integer getClosingDay() {
        return closingDay;
    }

    public Integer getDueDay() {
        return dueDay;
    }

    public boolean isActive() {
        return active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void rename(String name) {
        this.name = requireText(name, "name is required");
    }

    public void changeCreditLimit(Long creditLimitCents) {
        this.creditLimitCents = requireNonNegative(creditLimitCents, "creditLimitCents");
    }

    public void activate() {
        active = true;
    }

    public void deactivate() {
        active = false;
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private static String validateLastFour(String lastFour) {
        if (lastFour == null) {
            return null;
        }
        if (!lastFour.matches("[0-9]{4}")) {
            throw new IllegalArgumentException("lastFour must contain exactly four digits");
        }
        return lastFour;
    }

    private static Long requireNonNegative(Long value, String field) {
        Objects.requireNonNull(value, field + " is required");
        if (value < 0) {
            throw new IllegalArgumentException(field + " cannot be negative");
        }
        return value;
    }

    private static Integer requireDay(Integer day, String field) {
        Objects.requireNonNull(day, field + " is required");
        if (day < 1 || day > 31) {
            throw new IllegalArgumentException(field + " must be between 1 and 31");
        }
        return day;
    }
}
