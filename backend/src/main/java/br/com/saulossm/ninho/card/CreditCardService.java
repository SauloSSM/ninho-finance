package br.com.saulossm.ninho.card;

import br.com.saulossm.ninho.error.ResourceNotFoundException;
import br.com.saulossm.ninho.person.Person;
import br.com.saulossm.ninho.person.PersonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CreditCardService {

    private final CreditCardRepository repository;
    private final PersonRepository personRepository;

    public CreditCardService(CreditCardRepository repository, PersonRepository personRepository) {
        this.repository = repository;
        this.personRepository = personRepository;
    }

    @Transactional
    public CreditCardDtos.Response create(CreditCardDtos.CreateRequest request) {
        var card = new CreditCard(
                request.name(),
                request.institution(),
                requirePerson(request.holderId()),
                requirePerson(request.invoicePayerId()),
                requirePerson(request.defaultResponsiblePersonId()),
                request.lastFour(),
                request.creditLimitCents(),
                request.closingDay(),
                request.dueDay()
        );
        return CreditCardDtos.Response.from(repository.save(card));
    }

    @Transactional(readOnly = true)
    public List<CreditCardDtos.Response> findAll(Boolean active, Long holderId, Long invoicePayerId) {
        if (holderId != null && !personRepository.existsById(holderId)) {
            throw new ResourceNotFoundException("Person", holderId);
        }
        if (invoicePayerId != null && !personRepository.existsById(invoicePayerId)) {
            throw new ResourceNotFoundException("Person", invoicePayerId);
        }
        return repository.findFiltered(active, holderId, invoicePayerId).stream()
                .map(CreditCardDtos.Response::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CreditCardDtos.Response findById(Long id) {
        return CreditCardDtos.Response.from(require(id));
    }

    @Transactional
    public CreditCardDtos.Response changeStatus(Long id, CreditCardDtos.StatusRequest request) {
        var card = require(id);
        if (request.active()) {
            card.activate();
        } else {
            card.deactivate();
        }
        return CreditCardDtos.Response.from(card);
    }

    @Transactional
    public CreditCardDtos.Response changeLimit(Long id, CreditCardDtos.LimitRequest request) {
        var card = require(id);
        card.changeCreditLimit(request.creditLimitCents());
        return CreditCardDtos.Response.from(card);
    }

    private CreditCard require(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Credit card", id));
    }

    private Person requirePerson(Long id) {
        return personRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Person", id));
    }
}
