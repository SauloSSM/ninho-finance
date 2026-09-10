package br.com.saulossm.ninho.payment;

import br.com.saulossm.ninho.account.BankAccount;
import br.com.saulossm.ninho.account.BankAccountRepository;
import br.com.saulossm.ninho.bill.HouseholdBill;
import br.com.saulossm.ninho.bill.HouseholdBillRepository;
import br.com.saulossm.ninho.bill.HouseholdBillStatus;
import br.com.saulossm.ninho.card.CreditCardInvoice;
import br.com.saulossm.ninho.card.CreditCardInvoiceRepository;
import br.com.saulossm.ninho.error.BusinessRuleException;
import br.com.saulossm.ninho.error.ResourceNotFoundException;
import br.com.saulossm.ninho.expense.Expense;
import br.com.saulossm.ninho.expense.ExpenseRepository;
import br.com.saulossm.ninho.person.Person;
import br.com.saulossm.ninho.person.PersonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PaymentService {

    private final PaymentRepository repository;
    private final PersonRepository personRepository;
    private final BankAccountRepository accountRepository;
    private final ExpenseRepository expenseRepository;
    private final HouseholdBillRepository billRepository;
    private final CreditCardInvoiceRepository invoiceRepository;

    public PaymentService(
            PaymentRepository repository,
            PersonRepository personRepository,
            BankAccountRepository accountRepository,
            ExpenseRepository expenseRepository,
            HouseholdBillRepository billRepository,
            CreditCardInvoiceRepository invoiceRepository
    ) {
        this.repository = repository;
        this.personRepository = personRepository;
        this.accountRepository = accountRepository;
        this.expenseRepository = expenseRepository;
        this.billRepository = billRepository;
        this.invoiceRepository = invoiceRepository;
    }

    @Transactional
    public PaymentDtos.Response create(PaymentDtos.CreateRequest request) {
        validateExactlyOneTarget(request);
        Person payer = requirePerson(request.payerPersonId());
        BankAccount account = optionalAccount(request.bankAccountId());
        Expense expense = optionalExpense(request.expenseId());
        HouseholdBill bill = optionalBill(request.householdBillId());
        CreditCardInvoice invoice = optionalInvoice(request.creditCardInvoiceId());

        if (expense != null) {
            rejectOverpayment(
                    repository.sumAmountByExpenseId(expense.getId()),
                    request.amountCents(),
                    expense.getAmountCents(),
                    "Expense"
            );
        } else if (bill != null) {
            if (bill.getStatus() == HouseholdBillStatus.CANCELLED) {
                throw new BusinessRuleException("Cancelled household bill cannot be paid");
            }
            rejectOverpayment(
                    repository.sumAmountByHouseholdBillId(bill.getId()),
                    request.amountCents(),
                    bill.getEffectiveAmountCents(),
                    "Household bill"
            );
        } else {
            long calculatedTotal = expenseRepository.sumAmountByInvoiceId(invoice.getId());
            rejectOverpayment(
                    repository.sumAmountByCreditCardInvoiceId(invoice.getId()),
                    request.amountCents(),
                    calculatedTotal,
                    "Invoice"
            );
        }

        var payment = repository.saveAndFlush(new Payment(
                payer,
                account,
                request.method(),
                request.amountCents(),
                request.paidAt(),
                expense,
                bill,
                invoice,
                request.notes()
        ));

        if (bill != null) {
            long paid = repository.sumAmountByHouseholdBillId(bill.getId());
            if (paid == bill.getEffectiveAmountCents()) {
                bill.markPaid();
            } else {
                bill.markPartiallyPaid();
            }
        }
        return PaymentDtos.Response.from(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentDtos.Response> findAll(Long payerId, LocalDate from, LocalDate to) {
        validateRange(from, to);
        if (payerId != null && !personRepository.existsById(payerId)) {
            throw new ResourceNotFoundException("Person", payerId);
        }
        LocalDateTime start = from == null ? null : from.atStartOfDay();
        LocalDateTime end = to == null ? null : to.plusDays(1).atStartOfDay();
        return repository.findFiltered(payerId, start, end).stream().map(PaymentDtos.Response::from).toList();
    }

    @Transactional(readOnly = true)
    public PaymentDtos.Response findById(Long id) {
        return PaymentDtos.Response.from(repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", id)));
    }

    private static void rejectOverpayment(long alreadyPaid, long amount, long maximum, String target) {
        if (alreadyPaid > maximum || amount > maximum - alreadyPaid) {
            throw new BusinessRuleException(target + " payment exceeds outstanding amount");
        }
    }

    private static void validateExactlyOneTarget(PaymentDtos.CreateRequest request) {
        int targets = 0;
        targets += request.expenseId() == null ? 0 : 1;
        targets += request.householdBillId() == null ? 0 : 1;
        targets += request.creditCardInvoiceId() == null ? 0 : 1;
        if (targets != 1) {
            throw new BusinessRuleException("payment must have exactly one target");
        }
    }

    private Person requirePerson(Long id) {
        return personRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Person", id));
    }

    private BankAccount optionalAccount(Long id) {
        return id == null ? null : accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bank account", id));
    }

    private Expense optionalExpense(Long id) {
        return id == null ? null : expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense", id));
    }

    private HouseholdBill optionalBill(Long id) {
        return id == null ? null : billRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Household bill", id));
    }

    private CreditCardInvoice optionalInvoice(Long id) {
        return id == null ? null : invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", id));
    }

    private static void validateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("from must not be after to");
        }
    }
}
