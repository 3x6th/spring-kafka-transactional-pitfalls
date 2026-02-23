package dev.perevalov.kafkapitfals;

import dev.perevalov.kafkapitfals.config.BootstrapDataService;
import dev.perevalov.kafkapitfals.domain.OutboxStatus;
import dev.perevalov.kafkapitfals.repo.IdempotentLedgerEntryRepository;
import dev.perevalov.kafkapitfals.repo.NaiveLedgerEntryRepository;
import dev.perevalov.kafkapitfals.repo.OutboxEventRepository;
import dev.perevalov.kafkapitfals.repo.ProcessedEventRepository;
import dev.perevalov.kafkapitfals.scenario.outbox.OutboxRelayService;
import dev.perevalov.kafkapitfals.scenario.outbox.OutboxTransferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;

import java.math.BigDecimal;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

/**
 * Integration tests for outbox flow and idempotent consumer behavior.
 *
 * <p>Goal: verify producer-side atomicity improvement and consumer-side duplicate suppression.</p>
 */
@SpringBootTest(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.consumer.auto-offset-reset=earliest"
})
@EmbeddedKafka(partitions = 1, topics = {"transfers.events"})
class OutboxFlowIntegrationTest {

    @Autowired
    private BootstrapDataService bootstrapDataService;

    @Autowired
    private OutboxTransferService outboxTransferService;

    @Autowired
    private OutboxRelayService outboxRelayService;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private NaiveLedgerEntryRepository naiveLedgerEntryRepository;

    @Autowired
    private IdempotentLedgerEntryRepository idempotentLedgerEntryRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    /**
     * Restores deterministic baseline before each test.
     */
    @BeforeEach
    void resetState() {
        bootstrapDataService.resetAll();
    }

    /**
     * Verifies that outbox row is rolled back together with business transaction on failure.
     */
    @Test
    void outboxShouldRollbackTogetherWithBusinessTransaction() {
        assertThatThrownBy(() -> outboxTransferService.transferWithOutbox(1L, 2L, new BigDecimal("50.00"), true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("before DB commit");

        assertThat(outboxEventRepository.count()).isZero();
    }

    /**
     * Verifies duplicate publish window and demonstrates difference between naive and idempotent
     * consumers.
     */
    @Test
    void duplicatePublishShouldAffectNaiveConsumerButNotIdempotentOne() {
        String eventId = outboxTransferService.transferWithOutbox(1L, 2L, new BigDecimal("70.00"), false);

        assertThat(outboxEventRepository.countByStatus(OutboxStatus.NEW)).isEqualTo(1);

        assertThatThrownBy(() -> outboxRelayService.relayPending(10, true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("before outbox status update");

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(naiveLedgerEntryRepository.countByEventId(eventId)).isEqualTo(1)
        );

        assertThat(outboxEventRepository.countByStatus(OutboxStatus.NEW)).isEqualTo(1);

        outboxRelayService.relayPending(10, false);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            assertThat(naiveLedgerEntryRepository.countByEventId(eventId)).isGreaterThanOrEqualTo(2);
            assertThat(idempotentLedgerEntryRepository.countByEventId(eventId)).isEqualTo(1);
            assertThat(processedEventRepository.count()).isEqualTo(1);
        });

        assertThat(outboxEventRepository.countByStatus(OutboxStatus.SENT)).isEqualTo(1);
    }
}
