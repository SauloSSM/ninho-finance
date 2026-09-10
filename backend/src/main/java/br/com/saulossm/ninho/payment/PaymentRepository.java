package br.com.saulossm.ninho.payment;

import br.com.saulossm.ninho.bill.HouseholdBill;
import br.com.saulossm.ninho.card.CreditCardInvoice;
import br.com.saulossm.ninho.expense.Expense;
import br.com.saulossm.ninho.person.Person;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findAllByPayerPerson(Person payerPerson);

    List<Payment> findAllByExpense(Expense expense);

    List<Payment> findAllByHouseholdBill(HouseholdBill householdBill);

    List<Payment> findAllByCreditCardInvoice(CreditCardInvoice creditCardInvoice);
}
