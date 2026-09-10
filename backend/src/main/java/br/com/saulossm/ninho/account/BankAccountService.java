package br.com.saulossm.ninho.account;

import br.com.saulossm.ninho.error.ResourceNotFoundException;
import br.com.saulossm.ninho.person.Person;
import br.com.saulossm.ninho.person.PersonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BankAccountService {

    private final BankAccountRepository repository;
    private final PersonRepository personRepository;

    public BankAccountService(BankAccountRepository repository, PersonRepository personRepository) {
        this.repository = repository;
        this.personRepository = personRepository;
    }

    @Transactional
    public BankAccountDtos.Response create(BankAccountDtos.CreateRequest request) {
        var account = new BankAccount(
                request.name(), request.institution(), requireOwner(request.ownerId()), request.initialBalanceCents()
        );
        return BankAccountDtos.Response.from(repository.save(account));
    }

    @Transactional(readOnly = true)
    public List<BankAccountDtos.Response> findAll(Long ownerId, Boolean active) {
        List<BankAccount> accounts;
        if (ownerId != null) {
            var owner = requireOwner(ownerId);
            accounts = active == null
                    ? repository.findAllByOwner(owner)
                    : repository.findAllByOwnerAndActive(owner, active);
        } else {
            accounts = active == null ? repository.findAll() : repository.findAllByActive(active);
        }
        return accounts.stream().map(BankAccountDtos.Response::from).toList();
    }

    @Transactional(readOnly = true)
    public BankAccountDtos.Response findById(Long id) {
        return BankAccountDtos.Response.from(require(id));
    }

    @Transactional
    public BankAccountDtos.Response changeStatus(Long id, BankAccountDtos.StatusRequest request) {
        var account = require(id);
        if (request.active()) {
            account.activate();
        } else {
            account.deactivate();
        }
        return BankAccountDtos.Response.from(account);
    }

    private BankAccount require(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Bank account", id));
    }

    private Person requireOwner(Long id) {
        return personRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Person", id));
    }
}
