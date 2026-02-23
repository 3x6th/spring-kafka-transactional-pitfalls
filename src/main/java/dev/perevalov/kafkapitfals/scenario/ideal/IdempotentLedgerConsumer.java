package dev.perevalov.kafkapitfals.scenario.ideal;

import dev.perevalov.kafkapitfals.domain.IdempotentLedgerEntryEntity;
import dev.perevalov.kafkapitfals.domain.ProcessedEventEntity;
import dev.perevalov.kafkapitfals.domain.TransferEvent;
import dev.perevalov.kafkapitfals.repo.IdempotentLedgerEntryRepository;
import dev.perevalov.kafkapitfals.repo.ProcessedEventRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Idempotent consumer that protects business side effects from duplicate delivery.
 *
 * <p>Problem addressed: outbox relay can re-send the same event, and Kafka can re-deliver after
 * consumer restart or rebalance. Without deduplication, side effects become non-deterministic.</p>
 *
 * <p>Fix: before applying side effect, consumer checks {@code processed_events} by {@code eventId}.
 * If id is present, processing is skipped. If absent, side effect and marker insert are stored in one
 * local DB transaction.</p>
 */
@Component
public class IdempotentLedgerConsumer {

    private final ProcessedEventRepository processedEventRepository;
    private final IdempotentLedgerEntryRepository idempotentLedgerEntryRepository;

    /**
     * Creates idempotent consumer dependencies.
     *
     * @param processedEventRepository dedup storage with one row per processed event id
     * @param idempotentLedgerEntryRepository ledger storage for successfully applied events
     */
    public IdempotentLedgerConsumer(
            ProcessedEventRepository processedEventRepository,
            IdempotentLedgerEntryRepository idempotentLedgerEntryRepository
    ) {
        this.processedEventRepository = processedEventRepository;
        this.idempotentLedgerEntryRepository = idempotentLedgerEntryRepository;
    }

    /**
     * Applies transfer event exactly once from business perspective.
     *
     * <p>Problem addressed: duplicate event deliveries should not generate duplicate ledger rows.</p>
     *
     * <p>Fix: dedup check plus atomic write of ledger row and processed marker.</p>
     *
     * @param event transfer event received from Kafka
     */
    @KafkaListener(id = "idempotent-ledger-consumer", topics = "${app.kafka.transfer-topic}", groupId = "ledger-idempotent")
    @Transactional
    public void consume(TransferEvent event) {
        if (processedEventRepository.existsById(event.eventId())) {
            return;
        }

        idempotentLedgerEntryRepository.save(new IdempotentLedgerEntryEntity(
                event.eventId(),
                event.toAccountId(),
                event.amount(),
                Instant.now()
        ));
        processedEventRepository.save(new ProcessedEventEntity(event.eventId(), Instant.now()));
    }
}
