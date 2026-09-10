package br.com.saulossm.ninho.person;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PersonRepository extends JpaRepository<Person, Long> {

    List<Person> findAllByActiveTrue();

    List<Person> findAllByType(PersonType type);

    List<Person> findAllByTypeAndActiveTrue(PersonType type);
}
