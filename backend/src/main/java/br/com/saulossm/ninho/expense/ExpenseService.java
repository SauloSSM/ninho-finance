package br.com.saulossm.ninho.expense;

import br.com.saulossm.ninho.card.CreditCardInvoice;
import br.com.saulossm.ninho.card.CreditCardInvoiceRepository;
import br.com.saulossm.ninho.category.Category;
import br.com.saulossm.ninho.category.CategoryRepository;
import br.com.saulossm.ninho.error.BusinessRuleException;
import br.com.saulossm.ninho.error.ResourceNotFoundException;
import br.com.saulossm.ninho.person.Person;
import br.com.saulossm.ninho.person.PersonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

@Service
public class ExpenseService {

    private final ExpenseRepository repository;
    private final ExpenseAllocationRepository allocationRepository;
    private final CategoryRepository categoryRepository;
    private final PersonRepository personRepository;
    private final CreditCardInvoiceRepository invoiceRepository;

    public ExpenseService(
            ExpenseRepository repository,
            ExpenseAllocationRepository allocationRepository,
            CategoryRepository categoryRepository,
            PersonRepository personRepository,
            CreditCardInvoiceRepository invoiceRepository
    ) {
        this.repository = repository;
        this.allocationRepository = allocationRepository;
        this.categoryRepository = categoryRepository;
        this.personRepository = personRepository;
        this.invoiceRepository = invoiceRepository;
    }

    @Transactional
    public ExpenseDtos.Response create(ExpenseDtos.CreateRequest request) {
        validateAllocations(request);
        var expense = repository.save(new Expense(
                request.description(),
                requireCategory(request.categoryId()),
                request.amountCents(),
                request.scope(),
                request.occurredAt(),
                optionalInvoice(request.creditCardInvoiceId()),
                request.notes()
        ));
        for (var allocationRequest : request.allocations()) {
            Person person = requirePerson(allocationRequest.personId());
            allocationRepository.save(new ExpenseAllocation(expense, person, allocationRequest.amountCents()));
        }
        return toResponse(expense);
    }

    @Transactional(readOnly = true)
    public List<ExpenseDtos.Response> findAll(
            LocalDate from,
            LocalDate to,
            ExpenseScope scope,
            Long categoryId,
            Long invoiceId
    ) {
        validateRange(from, to);
        if (categoryId != null && !categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Category", categoryId);
        }
        if (invoiceId != null && !invoiceRepository.existsById(invoiceId)) {
            throw new ResourceNotFoundException("Invoice", invoiceId);
        }
        LocalDateTime start = from == null ? null : from.atStartOfDay();
        LocalDateTime end = to == null ? null : to.plusDays(1).atStartOfDay();
        return repository.findFiltered(start, end, scope, categoryId, invoiceId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ExpenseDtos.Response findById(Long id) {
        return toResponse(require(id));
    }

    ExpenseDtos.Response toResponse(Expense expense) {
        var allocations = allocationRepository.findAllByExpense(expense).stream()
                .map(ExpenseDtos.AllocationResponse::from)
                .toList();
        return new ExpenseDtos.Response(
                expense.getId(),
                expense.getDescription(),
                br.com.saulossm.ninho.category.CategoryDtos.Summary.from(expense.getCategory()),
                expense.getAmountCents(),
                expense.getScope(),
                expense.getOccurredAt(),
                ExpenseDtos.InvoiceSummary.from(expense.getCreditCardInvoice()),
                expense.getNotes(),
                allocations,
                expense.getCreatedAt()
        );
    }

    private static void validateAllocations(ExpenseDtos.CreateRequest request) {
        if (request.allocations() == null || request.allocations().isEmpty()) {
            throw new BusinessRuleException("at least one allocation is required");
        }
        var people = new HashSet<Long>();
        long total = 0;
        for (var allocation : request.allocations()) {
            if (!people.add(allocation.personId())) {
                throw new BusinessRuleException("a person cannot appear more than once in allocations");
            }
            try {
                total = Math.addExact(total, allocation.amountCents());
            } catch (ArithmeticException exception) {
                throw new BusinessRuleException("allocation total is too large");
            }
        }
        if (total != request.amountCents()) {
            throw new BusinessRuleException("allocation total must equal expense amount");
        }
    }

    private Expense require(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Expense", id));
    }

    private Category requireCategory(Long id) {
        return categoryRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Category", id));
    }

    private Person requirePerson(Long id) {
        return personRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Person", id));
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
