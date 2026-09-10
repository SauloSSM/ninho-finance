package br.com.saulossm.ninho.income;

import br.com.saulossm.ninho.account.BankAccount;
import br.com.saulossm.ninho.account.BankAccountRepository;
import br.com.saulossm.ninho.category.Category;
import br.com.saulossm.ninho.category.CategoryRepository;
import br.com.saulossm.ninho.error.ResourceNotFoundException;
import br.com.saulossm.ninho.person.Person;
import br.com.saulossm.ninho.person.PersonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class IncomeService {

    private final IncomeRepository repository;
    private final PersonRepository personRepository;
    private final BankAccountRepository accountRepository;
    private final CategoryRepository categoryRepository;

    public IncomeService(
            IncomeRepository repository,
            PersonRepository personRepository,
            BankAccountRepository accountRepository,
            CategoryRepository categoryRepository
    ) {
        this.repository = repository;
        this.personRepository = personRepository;
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public IncomeDtos.Response create(IncomeDtos.CreateRequest request) {
        var income = new Income(
                requirePerson(request.personId()),
                optionalAccount(request.bankAccountId()),
                requireCategory(request.categoryId()),
                request.description(),
                request.amountCents(),
                request.expectedDate()
        );
        return IncomeDtos.Response.from(repository.save(income));
    }

    @Transactional(readOnly = true)
    public List<IncomeDtos.Response> findAll(Long personId, IncomeStatus status, LocalDate from, LocalDate to) {
        validateRange(from, to);
        if (personId != null && !personRepository.existsById(personId)) {
            throw new ResourceNotFoundException("Person", personId);
        }
        return repository.findFiltered(personId, status, from, to).stream()
                .map(IncomeDtos.Response::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public IncomeDtos.Response findById(Long id) {
        return IncomeDtos.Response.from(require(id));
    }

    @Transactional
    public IncomeDtos.Response receive(Long id, IncomeDtos.ReceiveRequest request) {
        var income = require(id);
        if (request.bankAccountId() == null) {
            income.markReceived(request.receivedAt());
        } else {
            income.markReceived(request.receivedAt(), requireAccount(request.bankAccountId()));
        }
        return IncomeDtos.Response.from(income);
    }

    @Transactional
    public IncomeDtos.Response cancel(Long id) {
        var income = require(id);
        income.cancel();
        return IncomeDtos.Response.from(income);
    }

    private Income require(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Income", id));
    }

    private Person requirePerson(Long id) {
        return personRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Person", id));
    }

    private Category requireCategory(Long id) {
        return categoryRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Category", id));
    }

    private BankAccount requireAccount(Long id) {
        return accountRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Bank account", id));
    }

    private BankAccount optionalAccount(Long id) {
        return id == null ? null : requireAccount(id);
    }

    private static void validateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("from must not be after to");
        }
    }
}
