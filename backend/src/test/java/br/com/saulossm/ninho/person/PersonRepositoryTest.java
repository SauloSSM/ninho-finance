package br.com.saulossm.ninho.person;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PersonRepositoryTest {

    private static final Path DATABASE = createTemporaryDatabase();

    @Autowired
    private PersonRepository repository;

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + DATABASE);
    }

    @Test
    void savesPersonUsingDatabaseGeneratedValues() {
        var person = repository.saveAndFlush(new Person("Saulo", PersonType.USER));

        assertThat(person.getId()).isNotNull();
        assertThat(person.getName()).isEqualTo("Saulo");
        assertThat(person.getType()).isEqualTo(PersonType.USER);
        assertThat(person.isActive()).isTrue();
        assertThat(person.getCreatedAt()).isNotNull();
    }

    @Test
    void findsOnlyActivePeople() {
        var activeUser = repository.save(new Person("Active user", PersonType.USER));
        var activeExternal = repository.save(new Person("Active external", PersonType.EXTERNAL));
        var inactiveUser = new Person("Inactive user", PersonType.USER);
        inactiveUser.deactivate();
        repository.saveAndFlush(inactiveUser);

        assertThat(repository.findAllByActiveTrue())
                .containsExactlyInAnyOrder(activeUser, activeExternal)
                .doesNotContain(inactiveUser);
    }

    @Test
    void findsPeopleByType() {
        var user = repository.save(new Person("User", PersonType.USER));
        var external = repository.save(new Person("External", PersonType.EXTERNAL));
        repository.flush();

        assertThat(repository.findAllByType(PersonType.USER))
                .containsExactly(user)
                .doesNotContain(external);
        assertThat(repository.findAllByType(PersonType.EXTERNAL))
                .containsExactly(external)
                .doesNotContain(user);
    }

    @Test
    void findsOnlyActivePeopleByType() {
        var activeUser = repository.save(new Person("Active user", PersonType.USER));
        var inactiveUser = new Person("Inactive user", PersonType.USER);
        inactiveUser.deactivate();
        repository.save(inactiveUser);
        var activeExternal = repository.save(new Person("Active external", PersonType.EXTERNAL));
        repository.flush();

        assertThat(repository.findAllByTypeAndActiveTrue(PersonType.USER))
                .containsExactly(activeUser)
                .doesNotContain(inactiveUser, activeExternal);
    }

    private static Path createTemporaryDatabase() {
        try {
            var database = Files.createTempFile("ninho-person-repository-", ".db");
            database.toFile().deleteOnExit();
            return database;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not create the test database", exception);
        }
    }
}
