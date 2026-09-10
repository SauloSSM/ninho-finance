package br.com.saulossm.ninho.payment;

import br.com.saulossm.ninho.bill.HouseholdBill;
import br.com.saulossm.ninho.card.CreditCardInvoice;
import br.com.saulossm.ninho.expense.Expense;
import br.com.saulossm.ninho.person.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findAllByPayerPerson(Person payerPerson);

    List<Payment> findAllByExpense(Expense expense);

    List<Payment> findAllByHouseholdBill(HouseholdBill householdBill);

    List<Payment> findAllByCreditCardInvoice(CreditCardInvoice creditCardInvoice);

    @Query("""
            select payment from Payment payment
            where (:payerPersonId is null or payment.payerPerson.id = :payerPersonId)
              and (:fromDate is null or payment.paidAt >= :fromDate)
              and (:toDate is null or payment.paidAt < :toDate)
            order by payment.paidAt desc, payment.id desc
            """)
    List<Payment> findFiltered(
            @Param("payerPersonId") Long payerPersonId,
            @Param("fromDate") LocalDateTime from,
            @Param("toDate") LocalDateTime to
    );

    @Query("select coalesce(sum(payment.amountCents), 0) from Payment payment where payment.expense.id = :expenseId")
    Long sumAmountByExpenseId(@Param("expenseId") Long expenseId);

    @Query("select coalesce(sum(payment.amountCents), 0) from Payment payment where payment.householdBill.id = :billId")
    Long sumAmountByHouseholdBillId(@Param("billId") Long billId);

    @Query("select coalesce(sum(payment.amountCents), 0) from Payment payment where payment.creditCardInvoice.id = :invoiceId")
    Long sumAmountByCreditCardInvoiceId(@Param("invoiceId") Long invoiceId);
}
