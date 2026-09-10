package br.com.saulossm.ninho.card;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CreditCardInvoiceRepository extends JpaRepository<CreditCardInvoice, Long> {

    List<CreditCardInvoice> findAllByCreditCard(CreditCard creditCard);

    Optional<CreditCardInvoice> findByCreditCardAndReferenceYearAndReferenceMonth(
            CreditCard creditCard,
            Integer referenceYear,
            Integer referenceMonth
    );

    @Query("""
            select invoice from CreditCardInvoice invoice
            where (:creditCardId is null or invoice.creditCard.id = :creditCardId)
              and (:referenceYear is null or invoice.referenceYear = :referenceYear)
              and (:referenceMonth is null or invoice.referenceMonth = :referenceMonth)
            order by invoice.dueDate, invoice.id
            """)
    List<CreditCardInvoice> findFiltered(
            @Param("creditCardId") Long creditCardId,
            @Param("referenceYear") Integer referenceYear,
            @Param("referenceMonth") Integer referenceMonth
    );
}
