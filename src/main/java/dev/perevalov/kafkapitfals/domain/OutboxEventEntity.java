package dev.perevalov.kafkapitfals.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Outbox row stored in DB as part of producer transaction.
 *
 * <p>Problem addressed: direct producer dual write to DB and Kafka is not atomic. Outbox row serves
 * as reliable handoff between local DB transaction and asynchronous Kafka publish.</p>
 */
@Entity
@Table(name = "outbox_events")
public class OutboxEventEntity {

    @Id
    @Column(nullable = false, length = 80)
    private String id;

    @Column(nullable = false)
    private String topic;

    @Column(nullable = false)
    private String eventKey;

    @Lob
    @Column(nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant sentAt;

    /**
     * Protected constructor required by JPA.
     */
    protected OutboxEventEntity() {
    }

    /**
     * Creates new outbox event row.
     *
     * @param id event id
     * @param topic destination Kafka topic
     * @param eventKey Kafka key for partitioning and deduplication
     * @param payload serialized event payload
     * @param status current outbox status
     * @param createdAt row creation timestamp
     */
    public OutboxEventEntity(String id, String topic, String eventKey, String payload, OutboxStatus status, Instant createdAt) {
        this.id = id;
        this.topic = topic;
        this.eventKey = eventKey;
        this.payload = payload;
        this.status = status;
        this.createdAt = createdAt;
    }

    /**
     * Returns outbox row id.
     *
     * @return row id
     */
    public String getId() {
        return id;
    }

    /**
     * Returns destination Kafka topic.
     *
     * @return topic name
     */
    public String getTopic() {
        return topic;
    }

    /**
     * Returns Kafka key.
     *
     * @return event key
     */
    public String getEventKey() {
        return eventKey;
    }

    /**
     * Returns serialized payload.
     *
     * @return JSON payload
     */
    public String getPayload() {
        return payload;
    }

    /**
     * Returns current outbox status.
     *
     * @return status value
     */
    public OutboxStatus getStatus() {
        return status;
    }

    /**
     * Returns creation timestamp.
     *
     * @return creation instant
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Returns send timestamp.
     *
     * @return send instant or {@code null} if not sent yet
     */
    public Instant getSentAt() {
        return sentAt;
    }

    /**
     * Marks row as successfully sent by relay.
     *
     * <p>Problem addressed: explicit state transition allows retrying only pending rows.</p>
     *
     * @param now timestamp of successful publish acknowledgement
     */
    public void markSent(Instant now) {
        this.status = OutboxStatus.SENT;
        this.sentAt = now;
    }
}
