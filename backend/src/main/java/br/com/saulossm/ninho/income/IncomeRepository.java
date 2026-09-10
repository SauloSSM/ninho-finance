package br.com.saulossm.ninho.income;

import br.com.saulossm.ninho.person.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface IncomeRepository extends JpaRepository<Income, Long> {

    List<Income> findAllByPerson(Person person);

    List<Income> findAllByStatus(IncomeStatus status);

    List<Income> findAllByExpectedDateBetween(LocalDate start, LocalDate end);

    @Query("""
            select income from Income income
            where (:personId is null or income.person.id = :personId)
              and (:status is null or income.status = :status)
              and (:fromDate is null or income.expectedDate >= :fromDate)
              and (:toDate is null or income.expectedDate <= :toDate)
            order by income.expectedDate, income.id
            """)
    List<Income> findFiltered(
            @Param("personId") Long personId,
            @Param("status") IncomeStatus status,
            @Param("fromDate") LocalDate from,
            @Param("toDate") LocalDate to
    );

    @Query("""
            select coalesce(sum(income.amountCents), 0) from Income income
            where income.status <> br.com.saulossm.ninho.income.IncomeStatus.CANCELLED
              and income.expectedDate >= :start and income.expectedDate < :end
            """)
    Long sumExpectedInPeriod(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("""
            select coalesce(sum(income.amountCents), 0) from Income income
            where income.status = br.com.saulossm.ninho.income.IncomeStatus.RECEIVED
              and income.receivedAt >= :start and income.receivedAt < :end
            """)
    Long sumReceivedInPeriod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
