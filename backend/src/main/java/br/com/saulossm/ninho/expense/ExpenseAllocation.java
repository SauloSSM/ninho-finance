package br.com.saulossm.ninho.expense;

import br.com.saulossm.ninho.person.Person;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.Generated;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(
        name = "expense_allocations",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_expense_allocations_expense_person",
                columnNames = {"expense_id", "person_id"}
        )
)
public class ExpenseAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "INTEGER")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "expense_id", nullable = false)
    private Expense expense;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    @Column(name = "amount_cents", nullable = false, columnDefinition = "INTEGER")
    private Long amountCents;

    @Generated
    @Column(
            name = "created_at",
            nullable = false,
            insertable = false,
            updatable = false,
            columnDefinition = "TEXT"
    )
    private LocalDateTime createdAt;

    protected ExpenseAllocation() {
    }

    public ExpenseAllocation(Expense expense, Person person, Long amountCents) {
        this.expense = Objects.requireNonNull(expense, "expense is required");
        this.person = Objects.requireNonNull(person, "person is required");
        this.amountCents = requirePositive(amountCents);
        expense.registerAllocation(this);
    }

    public Long getId() {
        return id;
    }

    public Expense getExpense() {
        return expense;
    }

    public Person getPerson() {
        return person;
    }

    public Long getAmountCents() {
        return amountCents;
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
}
