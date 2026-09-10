package br.com.saulossm.ninho.payment;

import br.com.saulossm.ninho.person.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SettlementPaymentRepository extends JpaRepository<SettlementPayment, Long> {

    List<SettlementPayment> findAllByFromPerson(Person fromPerson);

    List<SettlementPayment> findAllByToPerson(Person toPerson);

    List<SettlementPayment> findAllByFromPersonAndToPerson(Person fromPerson, Person toPerson);

    @Query("""
            select settlement from SettlementPayment settlement
            where (:personId is null or settlement.fromPerson.id = :personId or settlement.toPerson.id = :personId)
              and (:fromDate is null or settlement.paidAt >= :fromDate)
              and (:toDate is null or settlement.paidAt < :toDate)
            order by settlement.paidAt desc, settlement.id desc
            """)
    List<SettlementPayment> findFiltered(
            @Param("personId") Long personId,
            @Param("fromDate") LocalDateTime from,
            @Param("toDate") LocalDateTime to
    );
}
