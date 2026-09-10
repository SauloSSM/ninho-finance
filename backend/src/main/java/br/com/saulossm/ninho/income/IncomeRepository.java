package br.com.saulossm.ninho.income;

import br.com.saulossm.ninho.person.Person;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface IncomeRepository extends JpaRepository<Income, Long> {

    List<Income> findAllByPerson(Person person);

    List<Income> findAllByStatus(IncomeStatus status);

    List<Income> findAllByExpectedDateBetween(LocalDate start, LocalDate end);
}
