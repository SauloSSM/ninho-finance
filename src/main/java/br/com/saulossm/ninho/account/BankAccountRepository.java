package br.com.saulossm.ninho.account;

import br.com.saulossm.ninho.person.Person;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {

    List<BankAccount> findAllByActiveTrue();

    List<BankAccount> findAllByOwner(Person owner);

    List<BankAccount> findAllByOwnerAndActiveTrue(Person owner);
}
