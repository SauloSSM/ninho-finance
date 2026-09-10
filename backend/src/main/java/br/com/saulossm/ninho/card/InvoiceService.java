package br.com.saulossm.ninho.card;

import br.com.saulossm.ninho.error.ConflictException;
import br.com.saulossm.ninho.error.ResourceNotFoundException;
import br.com.saulossm.ninho.expense.ExpenseRepository;
import br.com.saulossm.ninho.payment.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InvoiceService {

    private final CreditCardInvoiceRepository repository;
    private final CreditCardRepository cardRepository;
    private final ExpenseRepository expenseRepository;
    private final PaymentRepository paymentRepository;

    public InvoiceService(
            CreditCardInvoiceRepository repository,
            CreditCardRepository cardRepository,
            ExpenseRepository expenseRepository,
            PaymentRepository paymentRepository
    ) {
        this.repository = repository;
        this.cardRepository = cardRepository;
        this.expenseRepository = expenseRepository;
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public InvoiceDtos.Response create(InvoiceDtos.CreateRequest request) {
        var card = cardRepository.findById(request.creditCardId())
                .orElseThrow(() -> new ResourceNotFoundException("Credit card", request.creditCardId()));
        if (repository.findByCreditCardAndReferenceYearAndReferenceMonth(
                card, request.referenceYear(), request.referenceMonth()
        ).isPresent()) {
            throw new ConflictException("Invoice already exists for this card and reference month");
        }
        var invoice = repository.save(new CreditCardInvoice(
                card,
                request.referenceYear(),
                request.referenceMonth(),
                request.dueDate(),
                request.reportedTotalCents()
        ));
        return toResponse(invoice);
    }

    @Transactional(readOnly = true)
    public List<InvoiceDtos.Response> findAll(Long cardId, Integer year, Integer month) {
        validateMonth(month);
        if (cardId != null && !cardRepository.existsById(cardId)) {
            throw new ResourceNotFoundException("Credit card", cardId);
        }
        return repository.findFiltered(cardId, year, month).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public InvoiceDtos.Response findById(Long id) {
        return toResponse(require(id));
    }

    @Transactional
    public InvoiceDtos.Response close(Long id) {
        var invoice = require(id);
        invoice.close();
        return toResponse(invoice);
    }

    InvoiceDtos.Response toResponse(CreditCardInvoice invoice) {
        long calculated = expenseRepository.sumAmountByInvoiceId(invoice.getId());
        long paid = paymentRepository.sumAmountByCreditCardInvoiceId(invoice.getId());
        long outstanding = Math.max(calculated - paid, 0);
        var reported = invoice.getReportedTotalCents();
        Boolean reconciled = reported == null ? null : reported.equals(calculated);
        return new InvoiceDtos.Response(
                invoice.getId(),
                CreditCardDtos.Summary.from(invoice.getCreditCard()),
                invoice.getReferenceYear(),
                invoice.getReferenceMonth(),
                invoice.getDueDate(),
                invoice.getStatus(),
                reported,
                calculated,
                paid,
                outstanding,
                reconciled,
                invoice.getCreatedAt()
        );
    }

    private CreditCardInvoice require(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Invoice", id));
    }

    private static void validateMonth(Integer month) {
        if (month != null && (month < 1 || month > 12)) {
            throw new IllegalArgumentException("referenceMonth must be between 1 and 12");
        }
    }
}
