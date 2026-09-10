package br.com.saulossm.ninho.bill;

import br.com.saulossm.ninho.person.Person;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface HouseholdBillRepository extends JpaRepository<HouseholdBill, Long> {

    List<HouseholdBill> findAllByReferenceYearAndReferenceMonth(Integer referenceYear, Integer referenceMonth);

    List<HouseholdBill> findAllByStatus(HouseholdBillStatus status);

    List<HouseholdBill> findAllByDueDateBetween(LocalDate start, LocalDate end);

    List<HouseholdBill> findAllByResponsiblePerson(Person responsiblePerson);
}
