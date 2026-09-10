package br.com.saulossm.ninho.bill;

import br.com.saulossm.ninho.category.Category;
import br.com.saulossm.ninho.category.CategoryRepository;
import br.com.saulossm.ninho.error.BusinessRuleException;
import br.com.saulossm.ninho.error.ResourceNotFoundException;
import br.com.saulossm.ninho.payment.PaymentRepository;
import br.com.saulossm.ninho.person.Person;
import br.com.saulossm.ninho.person.PersonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class HouseholdBillService {

    private final HouseholdBillRepository repository;
    private final CategoryRepository categoryRepository;
    private final PersonRepository personRepository;
    private final PaymentRepository paymentRepository;

    public HouseholdBillService(
            HouseholdBillRepository repository,
            CategoryRepository categoryRepository,
            PersonRepository personRepository,
            PaymentRepository paymentRepository
    ) {
        this.repository = repository;
        this.categoryRepository = categoryRepository;
        this.personRepository = personRepository;
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public HouseholdBillDtos.Response create(HouseholdBillDtos.CreateRequest request) {
        var bill = new HouseholdBill(
                request.name(),
                requireCategory(request.categoryId()),
                requirePerson(request.responsiblePersonId()),
                request.referenceYear(),
                request.referenceMonth(),
                request.expectedAmountCents(),
                request.dueDate()
        );
        if (request.actualAmountCents() != null) {
            bill.setActualAmount(request.actualAmountCents());
        }
        return toResponse(repository.save(bill));
    }

    @Transactional(readOnly = true)
    public List<HouseholdBillDtos.Response> findAll(
            Integer year,
            Integer month,
            HouseholdBillStatus status,
            Long responsiblePersonId
    ) {
        validateMonth(month);
        if (responsiblePersonId != null && !personRepository.existsById(responsiblePersonId)) {
            throw new ResourceNotFoundException("Person", responsiblePersonId);
        }
        return repository.findFiltered(year, month, status, responsiblePersonId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public HouseholdBillDtos.Response findById(Long id) {
        return toResponse(require(id));
    }

    @Transactional
    public HouseholdBillDtos.Response updateAmount(Long id, HouseholdBillDtos.AmountRequest request) {
        var bill = require(id);
        if (request.expectedAmountCents() != null) {
            bill.updateExpectedAmount(request.expectedAmountCents());
        }
        if (request.actualAmountCents() != null) {
            bill.setActualAmount(request.actualAmountCents());
        }
        long paid = paymentRepository.sumAmountByHouseholdBillId(id);
        if (paid > bill.getEffectiveAmountCents()) {
            throw new BusinessRuleException("effective amount cannot be lower than the amount already paid");
        }
        if (paid > 0) {
            if (paid == bill.getEffectiveAmountCents()) {
                bill.markPaid();
            } else {
                bill.markPartiallyPaid();
            }
        }
        return toResponse(bill);
    }

    @Transactional
    public HouseholdBillDtos.Response cancel(Long id) {
        var bill = require(id);
        bill.cancel();
        return toResponse(bill);
    }

    HouseholdBillDtos.Response toResponse(HouseholdBill bill) {
        long effective = bill.getEffectiveAmountCents();
        long paid = paymentRepository.sumAmountByHouseholdBillId(bill.getId());
        long outstanding = Math.max(effective - paid, 0);
        boolean overdue = bill.getDueDate().isBefore(LocalDate.now())
                && bill.getStatus() != HouseholdBillStatus.PAID
                && bill.getStatus() != HouseholdBillStatus.CANCELLED;
        return new HouseholdBillDtos.Response(
                bill.getId(),
                bill.getName(),
                br.com.saulossm.ninho.category.CategoryDtos.Summary.from(bill.getCategory()),
                br.com.saulossm.ninho.person.PersonDtos.Summary.from(bill.getResponsiblePerson()),
                bill.getReferenceYear(),
                bill.getReferenceMonth(),
                bill.getExpectedAmountCents(),
                bill.getActualAmountCents(),
                effective,
                paid,
                outstanding,
                bill.getDueDate(),
                bill.getStatus(),
                overdue,
                bill.getCreatedAt()
        );
    }

    private HouseholdBill require(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Household bill", id));
    }

    private Category requireCategory(Long id) {
        return categoryRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Category", id));
    }

    private Person requirePerson(Long id) {
        return personRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Person", id));
    }

    private static void validateMonth(Integer month) {
        if (month != null && (month < 1 || month > 12)) {
            throw new IllegalArgumentException("referenceMonth must be between 1 and 12");
        }
    }
}
