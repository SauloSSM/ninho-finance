package br.com.saulossm.ninho.card;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CreditCardInvoiceRepository extends JpaRepository<CreditCardInvoice, Long> {

    List<CreditCardInvoice> findAllByCreditCard(CreditCard creditCard);

    Optional<CreditCardInvoice> findByCreditCardAndReferenceYearAndReferenceMonth(
            CreditCard creditCard,
            Integer referenceYear,
            Integer referenceMonth
    );
}
