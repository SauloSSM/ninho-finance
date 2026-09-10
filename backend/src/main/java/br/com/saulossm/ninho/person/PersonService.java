package br.com.saulossm.ninho.person;

import br.com.saulossm.ninho.error.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PersonService {

    private final PersonRepository repository;

    public PersonService(PersonRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public PersonDtos.Response create(PersonDtos.CreateRequest request) {
        return PersonDtos.Response.from(repository.save(new Person(request.name(), request.type())));
    }

    @Transactional(readOnly = true)
    public List<PersonDtos.Response> findAll(Boolean active) {
        var people = active == null ? repository.findAll() : repository.findAllByActive(active);
        return people.stream().map(PersonDtos.Response::from).toList();
    }

    @Transactional(readOnly = true)
    public PersonDtos.Response findById(Long id) {
        return PersonDtos.Response.from(require(id));
    }

    @Transactional
    public PersonDtos.Response changeStatus(Long id, PersonDtos.StatusRequest request) {
        var person = require(id);
        if (request.active()) {
            person.activate();
        } else {
            person.deactivate();
        }
        return PersonDtos.Response.from(person);
    }

    private Person require(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Person", id));
    }
}
