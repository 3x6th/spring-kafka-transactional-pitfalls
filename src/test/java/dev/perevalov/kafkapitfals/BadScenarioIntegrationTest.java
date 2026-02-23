package dev.perevalov.kafkapitfals;

import dev.perevalov.kafkapitfals.config.BootstrapDataService;
import dev.perevalov.kafkapitfals.repo.AccountRepository;
import dev.perevalov.kafkapitfals.repo.NaiveLedgerEntryRepository;
import dev.perevalov.kafkapitfals.scenario.bad.BadTransferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

/**
 * Integration tests for unsafe dual-write scenario.
 *
 * <p>Goal: prove that producer-side local transaction is not enough to guarantee consistency between
 * DB state and emitted Kafka events.</p>
 */
@SpringBootTest(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.consumer.auto-offset-reset=earliest"
})
@EmbeddedKafka(partitions = 1, topics = {"transfers.events"})
class BadScenarioIntegrationTest {

    @Autowired
    private BootstrapDataService bootstrapDataService;

    @Autowired
    private BadTransferService badTransferService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private NaiveLedgerEntryRepository naiveLedgerEntryRepository;

    /**
     * Restores deterministic baseline before each test.
     */
    @BeforeEach
    void resetState() {
        bootstrapDataService.resetAll();
    }

    /**
     * Verifies phantom event behavior:
     * DB rollback happens, but event is still consumed and side effect appears in naive ledger.
     */
    @Test
    void shouldProducePhantomEventWhenDbTransactionRollsBack() {
        assertThatThrownBy(() -> badTransferService.transferWithDualWrite(1L, 2L, 100.0, true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Simulated crash");

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(naiveLedgerEntryRepository.count()).isGreaterThanOrEqualTo(1)
        );

        assertThat(accountRepository.findById(1L).orElseThrow().getBalance())
                .isEqualByComparingTo("1000.00");
        assertThat(accountRepository.findById(2L).orElseThrow().getBalance())
                .isEqualByComparingTo("500.00");
    }
}
