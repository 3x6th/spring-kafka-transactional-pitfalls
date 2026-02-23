package dev.perevalov.kafkapitfals.scenario.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.perevalov.kafkapitfals.domain.OutboxEventEntity;
import dev.perevalov.kafkapitfals.domain.OutboxStatus;
import dev.perevalov.kafkapitfals.domain.TransferEvent;
import dev.perevalov.kafkapitfals.repo.OutboxEventRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Relays pending outbox records to Kafka.
 *
 * <p>Problem addressed: producer service should not directly publish to Kafka inside the same
 * transaction with business state update.</p>
 *
 * <p>Known risk: relay can crash after successful Kafka send and before outbox row is marked as
 * {@code SENT}. This causes re-send on retry. The repository demonstrates that this is expected and
 * should be handled by idempotent consumers.</p>
 */
@Service
public class OutboxRelayService {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, TransferEvent> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Creates relay dependencies for outbox polling, payload parsing and Kafka publishing.
     *
     * @param outboxEventRepository repository with pending outbox rows
     * @param kafkaTemplate Kafka producer used by relay
     * @param objectMapper JSON mapper used to deserialize stored payload
     */
    public OutboxRelayService(
            OutboxEventRepository outboxEventRepository,
            KafkaTemplate<String, TransferEvent> kafkaTemplate,
            ObjectMapper objectMapper
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Reads pending outbox rows, publishes events and marks them as sent.
     *
     * <p>Problem demonstrated: the method can fail after send and before status update. That is why
     * downstream consumers must be idempotent.</p>
     *
     * <p>Fix in overall architecture: combine this relay with consumer-side deduplication based on
     * event id.</p>
     *
     * @param batchSize max number of pending rows to process
     * @param failAfterFirstSend test hook to simulate crash window after first successful send
     * @return counts of selected and sent records
     */
    @Transactional
    public RelayResult relayPending(int batchSize, boolean failAfterFirstSend) {
        List<OutboxEventEntity> batch = outboxEventRepository.findByStatusOrderByCreatedAtAsc(
                OutboxStatus.NEW,
                PageRequest.of(0, batchSize)
        );

        int sent = 0;
        for (OutboxEventEntity outbox : batch) {
            TransferEvent event = readEvent(outbox.getPayload());
            sendSynchronously(outbox.getTopic(), outbox.getEventKey(), event);
            sent++;

            if (failAfterFirstSend && sent == 1) {
                throw new IllegalStateException(
                        "Simulated crash after Kafka send and before outbox status update. " +
                                "The same event will be sent again on retry."
                );
            }

            outbox.markSent(Instant.now());
        }

        return new RelayResult(batch.size(), sent);
    }

    /**
     * Restores transfer event from persisted JSON payload.
     *
     * @param payload outbox payload JSON
     * @return deserialized transfer event
     * @throws IllegalStateException when payload format is invalid
     */
    private TransferEvent readEvent(String payload) {
        try {
            return objectMapper.readValue(payload, TransferEvent.class);
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot deserialize outbox payload", ex);
        }
    }

    /**
     * Sends one event to Kafka and waits for broker acknowledgement.
     *
     * @param topic destination topic
     * @param key partitioning key and dedup key
     * @param event payload to send
     * @throws IllegalStateException when send fails
     */
    private void sendSynchronously(String topic, String key, TransferEvent event) {
        try {
            kafkaTemplate.send(topic, key, event).get(10, TimeUnit.SECONDS);
        } catch (Exception ex) {
            throw new IllegalStateException("Kafka publish failed", ex);
        }
    }

    /**
     * Relay execution counters used by API and tests.
     *
     * @param selected number of outbox rows read from DB
     * @param sent number of records successfully acknowledged by Kafka in this attempt
     */
    public record RelayResult(int selected, int sent) {
    }
}
