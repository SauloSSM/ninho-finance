package br.com.saulossm.ninho.card;

import br.com.saulossm.ninho.person.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CreditCardRepository extends JpaRepository<CreditCard, Long> {

    List<CreditCard> findAllByHolder(Person holder);

    List<CreditCard> findAllByInvoicePayer(Person invoicePayer);

    List<CreditCard> findAllByActiveTrue();

    List<CreditCard> findAllByActive(boolean active);

    List<CreditCard> findAllByHolderAndActive(Person holder, boolean active);

    List<CreditCard> findAllByInvoicePayerAndActive(Person invoicePayer, boolean active);

    @Query("""
            select card from CreditCard card
            where (:active is null or card.active = :active)
              and (:holderId is null or card.holder.id = :holderId)
              and (:invoicePayerId is null or card.invoicePayer.id = :invoicePayerId)
            order by card.name, card.id
            """)
    List<CreditCard> findFiltered(
            @Param("active") Boolean active,
            @Param("holderId") Long holderId,
            @Param("invoicePayerId") Long invoicePayerId
    );
}
