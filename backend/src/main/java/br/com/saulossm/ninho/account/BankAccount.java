package br.com.saulossm.ninho.account;

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
@Table(name = "bank_accounts")
public class BankAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "INTEGER")
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String institution;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private Person owner;

    @Column(name = "initial_balance_cents", nullable = false, columnDefinition = "INTEGER")
    private Long initialBalanceCents;

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

    protected BankAccount() {
    }

    public BankAccount(String name, String institution, Person owner, Long initialBalanceCents) {
        this.name = requireText(name, "name is required");
        this.institution = requireText(institution, "institution is required");
        this.owner = Objects.requireNonNull(owner, "owner is required");
        this.initialBalanceCents = requireInitialBalance(initialBalanceCents);
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

    public Person getOwner() {
        return owner;
    }

    public Long getInitialBalanceCents() {
        return initialBalanceCents;
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

    private static Long requireInitialBalance(Long initialBalanceCents) {
        Objects.requireNonNull(initialBalanceCents, "initialBalanceCents is required");
        if (initialBalanceCents < 0) {
            throw new IllegalArgumentException("initialBalanceCents cannot be negative");
        }
        return initialBalanceCents;
    }
}
