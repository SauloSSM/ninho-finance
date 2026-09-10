package br.com.saulossm.ninho.expense;

import br.com.saulossm.ninho.person.Person;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExpenseAllocationRepository extends JpaRepository<ExpenseAllocation, Long> {

    List<ExpenseAllocation> findAllByExpense(Expense expense);

    List<ExpenseAllocation> findAllByPerson(Person person);
}
