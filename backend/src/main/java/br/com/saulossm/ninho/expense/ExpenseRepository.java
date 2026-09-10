package br.com.saulossm.ninho.expense;

import br.com.saulossm.ninho.card.CreditCardInvoice;
import br.com.saulossm.ninho.category.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findAllByOccurredAtBetween(LocalDateTime start, LocalDateTime end);

    List<Expense> findAllByScope(ExpenseScope scope);

    List<Expense> findAllByCreditCardInvoice(CreditCardInvoice creditCardInvoice);

    List<Expense> findAllByCategory(Category category);

    @Query("""
            select expense from Expense expense
            where (:fromDate is null or expense.occurredAt >= :fromDate)
              and (:toDate is null or expense.occurredAt < :toDate)
              and (:scope is null or expense.scope = :scope)
              and (:categoryId is null or expense.category.id = :categoryId)
              and (:invoiceId is null or expense.creditCardInvoice.id = :invoiceId)
            order by expense.occurredAt desc, expense.id desc
            """)
    List<Expense> findFiltered(
            @Param("fromDate") LocalDateTime from,
            @Param("toDate") LocalDateTime to,
            @Param("scope") ExpenseScope scope,
            @Param("categoryId") Long categoryId,
            @Param("invoiceId") Long invoiceId
    );

    @Query("select coalesce(sum(expense.amountCents), 0) from Expense expense where expense.creditCardInvoice.id = :invoiceId")
    Long sumAmountByInvoiceId(@Param("invoiceId") Long invoiceId);

    @Query("""
            select coalesce(sum(expense.amountCents), 0) from Expense expense
            where expense.occurredAt >= :start and expense.occurredAt < :end
              and expense.scope = :scope
              and expense.creditCardInvoice is null
            """)
    Long sumNonCardAmountByPeriodAndScope(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("scope") ExpenseScope scope
    );
}
