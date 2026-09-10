package br.com.saulossm.ninho.bill;

import br.com.saulossm.ninho.person.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface HouseholdBillRepository extends JpaRepository<HouseholdBill, Long> {

    List<HouseholdBill> findAllByReferenceYearAndReferenceMonth(Integer referenceYear, Integer referenceMonth);

    List<HouseholdBill> findAllByStatus(HouseholdBillStatus status);

    List<HouseholdBill> findAllByDueDateBetween(LocalDate start, LocalDate end);

    List<HouseholdBill> findAllByResponsiblePerson(Person responsiblePerson);

    @Query("""
            select bill from HouseholdBill bill
            where (:referenceYear is null or bill.referenceYear = :referenceYear)
              and (:referenceMonth is null or bill.referenceMonth = :referenceMonth)
              and (:status is null or bill.status = :status)
              and (:responsiblePersonId is null or bill.responsiblePerson.id = :responsiblePersonId)
            order by bill.dueDate, bill.id
            """)
    List<HouseholdBill> findFiltered(
            @Param("referenceYear") Integer referenceYear,
            @Param("referenceMonth") Integer referenceMonth,
            @Param("status") HouseholdBillStatus status,
            @Param("responsiblePersonId") Long responsiblePersonId
    );
}
