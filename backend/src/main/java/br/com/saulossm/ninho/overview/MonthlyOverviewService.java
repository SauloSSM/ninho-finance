package br.com.saulossm.ninho.overview;

import br.com.saulossm.ninho.bill.HouseholdBill;
import br.com.saulossm.ninho.bill.HouseholdBillRepository;
import br.com.saulossm.ninho.bill.HouseholdBillStatus;
import br.com.saulossm.ninho.card.CreditCardInvoice;
import br.com.saulossm.ninho.card.CreditCardInvoiceRepository;
import br.com.saulossm.ninho.expense.ExpenseRepository;
import br.com.saulossm.ninho.expense.ExpenseScope;
import br.com.saulossm.ninho.income.IncomeRepository;
import br.com.saulossm.ninho.payment.PaymentRepository;
import br.com.saulossm.ninho.person.PersonDtos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
public class MonthlyOverviewService {

    private final IncomeRepository incomeRepository;
    private final HouseholdBillRepository billRepository;
    private final CreditCardInvoiceRepository invoiceRepository;
    private final ExpenseRepository expenseRepository;
    private final PaymentRepository paymentRepository;

    public MonthlyOverviewService(
            IncomeRepository incomeRepository,
            HouseholdBillRepository billRepository,
            CreditCardInvoiceRepository invoiceRepository,
            ExpenseRepository expenseRepository,
            PaymentRepository paymentRepository
    ) {
        this.incomeRepository = incomeRepository;
        this.billRepository = billRepository;
        this.invoiceRepository = invoiceRepository;
        this.expenseRepository = expenseRepository;
        this.paymentRepository = paymentRepository;
    }

    @Transactional(readOnly = true)
    public MonthlyOverviewDtos.Response getMonthly(Integer year, Integer month) {
        YearMonth period = requirePeriod(year, month);
        LocalDate startDate = period.atDay(1);
        LocalDate endDate = period.plusMonths(1).atDay(1);

        long expectedIncome = incomeRepository.sumExpectedInPeriod(startDate, endDate);
        long receivedIncome = incomeRepository.sumReceivedInPeriod(
                startDate.atStartOfDay(), endDate.atStartOfDay()
        );

        long billTotal = 0;
        long billPaid = 0;
        long billOutstanding = 0;
        long billOverdue = 0;
        for (var bill : billsFor(period)) {
            if (bill.getStatus() == HouseholdBillStatus.CANCELLED) {
                continue;
            }
            long effective = bill.getEffectiveAmountCents();
            long paid = paymentRepository.sumAmountByHouseholdBillId(bill.getId());
            long outstanding = Math.max(effective - paid, 0);
            billTotal += effective;
            billPaid += paid;
            billOutstanding += outstanding;
            if (isOverdue(bill)) {
                billOverdue += outstanding;
            }
        }

        long invoiceTotal = 0;
        long invoicePaid = 0;
        long invoiceOutstanding = 0;
        for (var invoice : invoicesFor(period)) {
            long calculated = expenseRepository.sumAmountByInvoiceId(invoice.getId());
            long paid = paymentRepository.sumAmountByCreditCardInvoiceId(invoice.getId());
            invoiceTotal += calculated;
            invoicePaid += paid;
            invoiceOutstanding += Math.max(calculated - paid, 0);
        }

        /*
         * Dashboard expense totals deliberately exclude card-linked expenses: those amounts already appear
         * under creditCards through their invoice. Household bills remain separate obligations by design.
         */
        long personalExpenses = expenseRepository.sumNonCardAmountByPeriodAndScope(
                startDate.atStartOfDay(), endDate.atStartOfDay(), ExpenseScope.PERSONAL
        );
        long householdExpenses = expenseRepository.sumNonCardAmountByPeriodAndScope(
                startDate.atStartOfDay(), endDate.atStartOfDay(), ExpenseScope.HOUSEHOLD
        );

        return new MonthlyOverviewDtos.Response(
                year,
                month,
                new MonthlyOverviewDtos.Income(expectedIncome, receivedIncome),
                new MonthlyOverviewDtos.HouseholdBills(billTotal, billPaid, billOutstanding, billOverdue),
                new MonthlyOverviewDtos.CreditCards(invoiceTotal, invoicePaid, invoiceOutstanding),
                new MonthlyOverviewDtos.Expenses(
                        personalExpenses,
                        householdExpenses,
                        personalExpenses + householdExpenses
                )
        );
    }

    @Transactional(readOnly = true)
    public List<MonthlyOverviewDtos.BillItem> getBills(Integer year, Integer month) {
        return billsFor(requirePeriod(year, month)).stream().map(this::toBillItem).toList();
    }

    @Transactional(readOnly = true)
    public List<MonthlyOverviewDtos.InvoiceItem> getInvoices(Integer year, Integer month) {
        return invoicesFor(requirePeriod(year, month)).stream().map(this::toInvoiceItem).toList();
    }

    private MonthlyOverviewDtos.BillItem toBillItem(HouseholdBill bill) {
        long effective = bill.getEffectiveAmountCents();
        long paid = paymentRepository.sumAmountByHouseholdBillId(bill.getId());
        return new MonthlyOverviewDtos.BillItem(
                bill.getId(),
                bill.getName(),
                effective,
                paid,
                Math.max(effective - paid, 0),
                bill.getDueDate(),
                bill.getStatus(),
                isOverdue(bill),
                PersonDtos.Summary.from(bill.getResponsiblePerson())
        );
    }

    private MonthlyOverviewDtos.InvoiceItem toInvoiceItem(CreditCardInvoice invoice) {
        long calculated = expenseRepository.sumAmountByInvoiceId(invoice.getId());
        long paid = paymentRepository.sumAmountByCreditCardInvoiceId(invoice.getId());
        Long reported = invoice.getReportedTotalCents();
        return new MonthlyOverviewDtos.InvoiceItem(
                invoice.getId(),
                invoice.getCreditCard().getId(),
                invoice.getCreditCard().getName(),
                invoice.getDueDate(),
                invoice.getStatus(),
                calculated,
                paid,
                Math.max(calculated - paid, 0),
                reported,
                reported == null ? null : reported.equals(calculated)
        );
    }

    private List<HouseholdBill> billsFor(YearMonth period) {
        return billRepository.findFiltered(period.getYear(), period.getMonthValue(), null, null);
    }

    private List<CreditCardInvoice> invoicesFor(YearMonth period) {
        return invoiceRepository.findFiltered(null, period.getYear(), period.getMonthValue());
    }

    private static boolean isOverdue(HouseholdBill bill) {
        return bill.getDueDate().isBefore(LocalDate.now())
                && bill.getStatus() != HouseholdBillStatus.PAID
                && bill.getStatus() != HouseholdBillStatus.CANCELLED;
    }

    private static YearMonth requirePeriod(Integer year, Integer month) {
        if (year == null || year < 1) {
            throw new IllegalArgumentException("year must be positive");
        }
        if (month == null || month < 1 || month > 12) {
            throw new IllegalArgumentException("month must be between 1 and 12");
        }
        return YearMonth.of(year, month);
    }
}
