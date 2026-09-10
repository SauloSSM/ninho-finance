package br.com.saulossm.ninho.expense;

import br.com.saulossm.ninho.card.CreditCardInvoice;
import br.com.saulossm.ninho.category.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findAllByOccurredAtBetween(LocalDateTime start, LocalDateTime end);

    List<Expense> findAllByScope(ExpenseScope scope);

    List<Expense> findAllByCreditCardInvoice(CreditCardInvoice creditCardInvoice);

    List<Expense> findAllByCategory(Category category);
}
