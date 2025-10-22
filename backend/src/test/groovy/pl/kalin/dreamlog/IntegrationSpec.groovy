package pl.kalin.dreamlog

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.spock.Testcontainers
import spock.lang.Shared
import spock.lang.Specification

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
abstract class IntegrationSpec extends Specification {

    @Shared
    static PostgreSQLContainer postgres

    def setupSpec() {
        if (postgres == null) {
            postgres = new PostgreSQLContainer("postgres:17-alpine")
                .withDatabaseName("dreamlog_test")
                .withUsername("test")
                .withPassword("test")
            postgres.start()
        }
    }

    def cleanupSpec() {
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> postgres.getJdbcUrl())
        registry.add("spring.datasource.username", () -> postgres.getUsername())
        registry.add("spring.datasource.password", () -> postgres.getPassword())
        registry.add("spring.flyway.url", () -> postgres.getJdbcUrl())
        registry.add("spring.flyway.user", () -> postgres.getUsername())
        registry.add("spring.flyway.password", () -> postgres.getPassword())
    }
}
