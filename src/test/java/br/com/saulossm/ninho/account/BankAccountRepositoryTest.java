package br.com.saulossm.ninho.account;

import br.com.saulossm.ninho.person.Person;
import br.com.saulossm.ninho.person.PersonRepository;
import br.com.saulossm.ninho.person.PersonType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BankAccountRepositoryTest {

    private static final Path DATABASE = createTemporaryDatabase();

    @Autowired
    private BankAccountRepository repository;

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + DATABASE);
    }

    @Test
    void persistsBankAccountWithOwnerAndDatabaseGeneratedValues() {
        var owner = personRepository.saveAndFlush(new Person("Saulo", PersonType.USER));

        var account = repository.saveAndFlush(
                new BankAccount("Nubank do Saulo", "Nubank", owner, 50000L)
        );
        entityManager.clear();

        var persistedAccount = repository.findById(account.getId()).orElseThrow();
        assertThat(persistedAccount.getId()).isNotNull();
        assertThat(persistedAccount.getName()).isEqualTo("Nubank do Saulo");
        assertThat(persistedAccount.getInstitution()).isEqualTo("Nubank");
        assertThat(persistedAccount.getOwner().getId()).isEqualTo(owner.getId());
        assertThat(persistedAccount.getOwner().getName()).isEqualTo("Saulo");
        assertThat(persistedAccount.getInitialBalanceCents()).isEqualTo(50000L);
        assertThat(persistedAccount.isActive()).isTrue();
        assertThat(persistedAccount.getCreatedAt()).isNotNull();
    }

    @Test
    void findsBankAccountsByOwner() {
        var mother = personRepository.save(new Person("Mãe", PersonType.USER));
        var saulo = personRepository.save(new Person("Saulo", PersonType.USER));
        var mothersAccount = repository.save(new BankAccount("Caixa da Mãe", "Caixa", mother, 0L));
        var nubank = repository.save(new BankAccount("Nubank do Saulo", "Nubank", saulo, 0L));
        var caixa = repository.saveAndFlush(new BankAccount("Caixa do Saulo", "Caixa", saulo, 0L));

        assertThat(repository.findAllByOwner(mother))
                .containsExactly(mothersAccount)
                .doesNotContain(nubank, caixa);
        assertThat(repository.findAllByOwner(saulo))
                .containsExactlyInAnyOrder(nubank, caixa)
                .doesNotContain(mothersAccount);
    }

    @Test
    void findsOnlyActiveBankAccounts() {
        var owner = personRepository.save(new Person("Saulo", PersonType.USER));
        var activeAccount = repository.save(new BankAccount("Nubank do Saulo", "Nubank", owner, 0L));
        var inactiveAccount = new BankAccount("Caixa do Saulo", "Caixa", owner, 0L);
        inactiveAccount.deactivate();
        repository.saveAndFlush(inactiveAccount);

        assertThat(repository.findAllByActiveTrue())
                .containsExactly(activeAccount)
                .doesNotContain(inactiveAccount);
    }

    @Test
    void findsOnlyActiveBankAccountsByOwner() {
        var mother = personRepository.save(new Person("Mãe", PersonType.USER));
        var saulo = personRepository.save(new Person("Saulo", PersonType.USER));
        var activeSauloAccount = repository.save(
                new BankAccount("Nubank do Saulo", "Nubank", saulo, 0L)
        );
        var inactiveSauloAccount = new BankAccount("Caixa do Saulo", "Caixa", saulo, 0L);
        inactiveSauloAccount.deactivate();
        repository.save(inactiveSauloAccount);
        var mothersAccount = repository.saveAndFlush(
                new BankAccount("Caixa da Mãe", "Caixa", mother, 0L)
        );

        assertThat(repository.findAllByOwnerAndActiveTrue(saulo))
                .containsExactly(activeSauloAccount)
                .doesNotContain(inactiveSauloAccount, mothersAccount);
    }

    @Test
    void persistsActivationChanges() {
        var owner = personRepository.save(new Person("Saulo", PersonType.USER));
        var account = repository.saveAndFlush(
                new BankAccount("Caixa do Saulo", "Caixa", owner, 0L)
        );

        account.deactivate();
        repository.flush();
        entityManager.clear();

        var inactiveAccount = repository.findById(account.getId()).orElseThrow();
        assertThat(inactiveAccount.isActive()).isFalse();

        inactiveAccount.activate();
        repository.flush();
        entityManager.clear();

        assertThat(repository.findById(account.getId()).orElseThrow().isActive()).isTrue();
    }

    @Test
    void rejectsInvalidDomainData() {
        var owner = new Person("Saulo", PersonType.USER);

        assertThatThrownBy(() -> new BankAccount(null, "Nubank", owner, 0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("name is required");
        assertThatThrownBy(() -> new BankAccount(" ", "Nubank", owner, 0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("name is required");
        assertThatThrownBy(() -> new BankAccount("Nubank do Saulo", null, owner, 0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("institution is required");
        assertThatThrownBy(() -> new BankAccount("Nubank do Saulo", " ", owner, 0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("institution is required");
        assertThatThrownBy(() -> new BankAccount("Nubank do Saulo", "Nubank", null, 0L))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("owner is required");
        assertThatThrownBy(() -> new BankAccount("Nubank do Saulo", "Nubank", owner, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("initialBalanceCents is required");
        assertThatThrownBy(() -> new BankAccount("Nubank do Saulo", "Nubank", owner, -1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("initialBalanceCents cannot be negative");
        assertThatThrownBy(() -> new BankAccount("Valid", "Bank", owner, 0L).rename(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("name is required");
    }

    @Test
    void rejectsBankAccountWithoutValidPerson() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                INSERT INTO bank_accounts
                    (name, institution, owner_id, initial_balance_cents, active)
                VALUES (?, ?, ?, ?, ?)
                """,
                "Conta sem dono", "Caixa", Long.MAX_VALUE, 0L, 1
        ))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("FOREIGN KEY constraint failed");
    }

    private static Path createTemporaryDatabase() {
        try {
            var database = Files.createTempFile("ninho-bank-account-repository-", ".db");
            database.toFile().deleteOnExit();
            return database;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not create the test database", exception);
        }
    }
}
