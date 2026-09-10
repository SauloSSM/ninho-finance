package br.com.saulossm.ninho.payment;

import br.com.saulossm.ninho.error.ResourceNotFoundException;
import br.com.saulossm.ninho.person.Person;
import br.com.saulossm.ninho.person.PersonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SettlementService {

    private final SettlementPaymentRepository repository;
    private final PersonRepository personRepository;

    public SettlementService(SettlementPaymentRepository repository, PersonRepository personRepository) {
        this.repository = repository;
        this.personRepository = personRepository;
    }

    @Transactional
    public SettlementDtos.Response create(SettlementDtos.CreateRequest request) {
        var settlement = new SettlementPayment(
                requirePerson(request.fromPersonId()),
                requirePerson(request.toPersonId()),
                request.amountCents(),
                request.paidAt(),
                request.notes()
        );
        return SettlementDtos.Response.from(repository.save(settlement));
    }

    @Transactional(readOnly = true)
    public List<SettlementDtos.Response> findAll(Long personId, LocalDate from, LocalDate to) {
        validateRange(from, to);
        if (personId != null && !personRepository.existsById(personId)) {
            throw new ResourceNotFoundException("Person", personId);
        }
        LocalDateTime start = from == null ? null : from.atStartOfDay();
        LocalDateTime end = to == null ? null : to.plusDays(1).atStartOfDay();
        return repository.findFiltered(personId, start, end).stream()
                .map(SettlementDtos.Response::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public SettlementDtos.Response findById(Long id) {
        return SettlementDtos.Response.from(repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Settlement", id)));
    }

    private Person requirePerson(Long id) {
        return personRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Person", id));
    }

    private static void validateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("from must not be after to");
        }
    }
}
