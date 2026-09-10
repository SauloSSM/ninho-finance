package br.com.saulossm.ninho.payment;

import br.com.saulossm.ninho.persistence.LocalDateTimeStringConverter;
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

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "settlement_payments")
public class SettlementPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "INTEGER")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_person_id", nullable = false)
    private Person fromPerson;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_person_id", nullable = false)
    private Person toPerson;

    @Column(name = "amount_cents", nullable = false, columnDefinition = "INTEGER")
    private Long amountCents;

    @Convert(converter = LocalDateTimeStringConverter.class)
    @Column(name = "paid_at", nullable = false, columnDefinition = "TEXT")
    private LocalDateTime paidAt;

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

    protected SettlementPayment() {
    }

    public SettlementPayment(
            Person fromPerson,
            Person toPerson,
            Long amountCents,
            LocalDateTime paidAt,
            String notes
    ) {
        this.fromPerson = Objects.requireNonNull(fromPerson, "fromPerson is required");
        this.toPerson = Objects.requireNonNull(toPerson, "toPerson is required");
        requireDifferentPeople(fromPerson, toPerson);
        this.amountCents = requirePositive(amountCents);
        this.paidAt = Objects.requireNonNull(paidAt, "paidAt is required");
        this.notes = notes;
    }

    public Long getId() {
        return id;
    }

    public Person getFromPerson() {
        return fromPerson;
    }

    public Person getToPerson() {
        return toPerson;
    }

    public Long getAmountCents() {
        return amountCents;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public String getNotes() {
        return notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    private static void requireDifferentPeople(Person fromPerson, Person toPerson) {
        boolean sameInstance = fromPerson == toPerson;
        boolean samePersistentPerson = fromPerson.getId() != null
                && toPerson.getId() != null
                && fromPerson.getId().equals(toPerson.getId());
        if (sameInstance || samePersistentPerson) {
            throw new IllegalArgumentException("fromPerson and toPerson must be different");
        }
    }

    private static Long requirePositive(Long amountCents) {
        Objects.requireNonNull(amountCents, "amountCents is required");
        if (amountCents <= 0) {
            throw new IllegalArgumentException("amountCents must be positive");
        }
        return amountCents;
    }
}
