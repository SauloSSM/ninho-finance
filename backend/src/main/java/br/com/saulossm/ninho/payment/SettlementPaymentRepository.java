package br.com.saulossm.ninho.payment;

import br.com.saulossm.ninho.person.Person;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SettlementPaymentRepository extends JpaRepository<SettlementPayment, Long> {

    List<SettlementPayment> findAllByFromPerson(Person fromPerson);

    List<SettlementPayment> findAllByToPerson(Person toPerson);

    List<SettlementPayment> findAllByFromPersonAndToPerson(Person fromPerson, Person toPerson);
}
