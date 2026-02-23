package dev.perevalov.kafkapitfals.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Dedup marker for idempotent consumer processing.
 *
 * <p>Problem addressed: at-least-once delivery can replay events. Storing processed ids guarantees
 * business side effect is applied once.</p>
 */
@Entity
@Table(name = "processed_events")
public class ProcessedEventEntity {

    @Id
    @Column(nullable = false, length = 80)
    private String eventId;

    @Column(nullable = false)
    private Instant processedAt;

    /**
     * Protected constructor required by JPA.
     */
    protected ProcessedEventEntity() {
    }

    /**
     * Creates dedup marker row.
     *
     * @param eventId processed event id
     * @param processedAt processing timestamp
     */
    public ProcessedEventEntity(String eventId, Instant processedAt) {
        this.eventId = eventId;
        this.processedAt = processedAt;
    }

    /**
     * Returns processed event id.
     *
     * @return event id
     */
    public String getEventId() {
        return eventId;
    }

    /**
     * Returns processing timestamp.
     *
     * @return processing instant
     */
    public Instant getProcessedAt() {
        return processedAt;
    }
}
