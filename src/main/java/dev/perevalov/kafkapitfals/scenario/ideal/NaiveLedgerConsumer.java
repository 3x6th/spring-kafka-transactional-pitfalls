package dev.perevalov.kafkapitfals.scenario.ideal;

import dev.perevalov.kafkapitfals.domain.NaiveLedgerEntryEntity;
import dev.perevalov.kafkapitfals.domain.TransferEvent;
import dev.perevalov.kafkapitfals.repo.NaiveLedgerEntryRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Intentionally naive consumer used as a contrast baseline.
 *
 * <p>Problem: Kafka delivery is at-least-once under failures. Without deduplication, retries or
 * duplicate publishes produce duplicate side effects in the ledger.</p>
 *
 * <p>Fix in this repository: see {@link IdempotentLedgerConsumer}.</p>
 */
@Component
public class NaiveLedgerConsumer {

    private final NaiveLedgerEntryRepository naiveLedgerEntryRepository;

    /**
     * Creates consumer with repository used to persist applied ledger entries.
     *
     * @param naiveLedgerEntryRepository repository that stores every consumed event as-is
     */
    public NaiveLedgerConsumer(NaiveLedgerEntryRepository naiveLedgerEntryRepository) {
        this.naiveLedgerEntryRepository = naiveLedgerEntryRepository;
    }

    /**
     * Applies incoming event without any dedup guard.
     *
     * <p>Problem demonstrated: the same {@code eventId} may be processed multiple times and persisted
     * multiple times.</p>
     *
     * @param event transfer event consumed from Kafka
     */
    @KafkaListener(id = "naive-ledger-consumer", topics = "${app.kafka.transfer-topic}", groupId = "ledger-naive")
    @Transactional
    public void consume(TransferEvent event) {
        naiveLedgerEntryRepository.save(new NaiveLedgerEntryEntity(
                event.eventId(),
                event.toAccountId(),
                event.amount(),
                Instant.now()
        ));
    }
}
