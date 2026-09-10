package br.com.saulossm.ninho.card;

import br.com.saulossm.ninho.person.Person;
import br.com.saulossm.ninho.person.PersonRepository;
import br.com.saulossm.ninho.person.PersonType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CreditCardRepositoryTest {

    private static final Path DATABASE = createTemporaryDatabase();

    @Autowired
    private CreditCardRepository cardRepository;

    @Autowired
    private CreditCardInvoiceRepository invoiceRepository;

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private EntityManager entityManager;

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + DATABASE);
    }

    @Test
    void persistsCardWithDistinctHolderPayerAndDefaultResponsiblePerson() {
        var holder = personRepository.save(new Person("Madrinha", PersonType.EXTERNAL));
        var payer = personRepository.save(new Person("Mãe", PersonType.USER));
        var responsible = personRepository.saveAndFlush(new Person("Saulo", PersonType.USER));

        var card = cardRepository.saveAndFlush(new CreditCard(
                "Itaú Madrinha", "Itaú", holder, payer, responsible, "1234", 500000L, 5, 12
        ));
        entityManager.clear();

        var persisted = cardRepository.findById(card.getId()).orElseThrow();
        assertThat(persisted.getHolder().getId()).isEqualTo(holder.getId());
        assertThat(persisted.getInvoicePayer().getId()).isEqualTo(payer.getId());
        assertThat(persisted.getDefaultResponsiblePerson().getId()).isEqualTo(responsible.getId());
        assertThat(persisted.getCreditLimitCents()).isEqualTo(500000L);
        assertThat(persisted.getLastFour()).isEqualTo("1234");
        assertThat(persisted.isActive()).isTrue();
        assertThat(persisted.getCreatedAt()).isNotNull();
    }

    @Test
    void persistsCardDomainChanges() {
        var owner = personRepository.saveAndFlush(new Person("Saulo", PersonType.USER));
        var card = cardRepository.saveAndFlush(card("Nubank", owner, owner));

        card.rename("Nubank Ultravioleta");
        card.changeCreditLimit(750000L);
        card.deactivate();
        cardRepository.flush();
        entityManager.clear();

        var inactive = cardRepository.findById(card.getId()).orElseThrow();
        assertThat(inactive.getName()).isEqualTo("Nubank Ultravioleta");
        assertThat(inactive.getCreditLimitCents()).isEqualTo(750000L);
        assertThat(inactive.isActive()).isFalse();

        inactive.activate();
        cardRepository.flush();
        entityManager.clear();
        assertThat(cardRepository.findById(card.getId()).orElseThrow().isActive()).isTrue();
    }

    @Test
    void rejectsInvalidLimitAndBillingDays() {
        var owner = new Person("Saulo", PersonType.USER);

        assertThatThrownBy(() -> new CreditCard("N", "Bank", owner, owner, owner, null, -1L, 1, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("creditLimitCents cannot be negative");
        assertThatThrownBy(() -> new CreditCard("N", "Bank", owner, owner, owner, null, 0L, 0, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("closingDay must be between 1 and 31");
        assertThatThrownBy(() -> new CreditCard("N", "Bank", owner, owner, owner, null, 0L, 1, 32))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("dueDay must be between 1 and 31");
        assertThatThrownBy(() -> card("N", owner, owner).changeCreditLimit(-1L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CreditCard(
                "N", "Bank", owner, owner, owner, "1234567890123456", 0L, 1, 1
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("lastFour must contain exactly four digits");
    }

    @Test
    void findsCardsByHolderPayerAndActiveState() {
        var holder = personRepository.save(new Person("Madrinha", PersonType.EXTERNAL));
        var mother = personRepository.save(new Person("Mãe", PersonType.USER));
        var saulo = personRepository.save(new Person("Saulo", PersonType.USER));
        var itau = cardRepository.save(card("Itaú", holder, mother));
        var nubank = cardRepository.save(card("Nubank", saulo, saulo));
        var inactive = card("DM", mother, mother);
        inactive.deactivate();
        cardRepository.saveAndFlush(inactive);

        assertThat(cardRepository.findAllByHolder(holder)).containsExactly(itau);
        assertThat(cardRepository.findAllByInvoicePayer(mother)).containsExactlyInAnyOrder(itau, inactive);
        assertThat(cardRepository.findAllByActiveTrue()).containsExactlyInAnyOrder(itau, nubank);
    }

    @Test
    void persistsOptionalReportedTotalAndInvoiceStatusChanges() {
        var owner = personRepository.save(new Person("Saulo", PersonType.USER));
        var card = cardRepository.saveAndFlush(card("Nubank", owner, owner));
        var withoutReportedTotal = invoiceRepository.save(new CreditCardInvoice(
                card, 2026, 9, LocalDate.of(2026, 9, 15), null
        ));
        var invoice = invoiceRepository.saveAndFlush(new CreditCardInvoice(
                card, 2026, 10, LocalDate.of(2026, 10, 15), 185900L
        ));

        invoice.close();
        invoiceRepository.flush();
        entityManager.clear();

        var closed = invoiceRepository.findById(invoice.getId()).orElseThrow();
        assertThat(closed.getStatus()).isEqualTo(InvoiceStatus.CLOSED);
        assertThat(closed.getReportedTotalCents()).isEqualTo(185900L);
        assertThat(invoiceRepository.findById(withoutReportedTotal.getId()).orElseThrow().getReportedTotalCents())
                .isNull();
        assertThat(invoiceRepository.findAllByCreditCard(card)).hasSize(2);
        assertThat(invoiceRepository.findByCreditCardAndReferenceYearAndReferenceMonth(card, 2026, 10))
                .contains(closed);

        closed.reopen();
        invoiceRepository.flush();
        assertThat(closed.getStatus()).isEqualTo(InvoiceStatus.OPEN);
    }

    @Test
    void rejectsDuplicateInvoiceReferenceForSameCard() {
        var owner = personRepository.save(new Person("Saulo", PersonType.USER));
        var card = cardRepository.saveAndFlush(card("Nubank", owner, owner));
        invoiceRepository.saveAndFlush(new CreditCardInvoice(
                card, 2026, 9, LocalDate.of(2026, 9, 15), null
        ));

        assertThatThrownBy(() -> invoiceRepository.saveAndFlush(new CreditCardInvoice(
                card, 2026, 9, LocalDate.of(2026, 9, 16), 100L
        )))
                .isInstanceOf(JpaSystemException.class)
                .hasMessageContaining("UNIQUE constraint failed");
    }

    private static CreditCard card(String name, Person holder, Person payer) {
        return new CreditCard(name, "Bank", holder, payer, holder, null, 100000L, 5, 12);
    }

    private static Path createTemporaryDatabase() {
        try {
            var database = Files.createTempFile("ninho-credit-card-repository-", ".db");
            database.toFile().deleteOnExit();
            return database;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not create the test database", exception);
        }
    }
}
