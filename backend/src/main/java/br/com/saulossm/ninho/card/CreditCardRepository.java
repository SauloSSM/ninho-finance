package br.com.saulossm.ninho.card;

import br.com.saulossm.ninho.person.Person;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CreditCardRepository extends JpaRepository<CreditCard, Long> {

    List<CreditCard> findAllByHolder(Person holder);

    List<CreditCard> findAllByInvoicePayer(Person invoicePayer);

    List<CreditCard> findAllByActiveTrue();
}
