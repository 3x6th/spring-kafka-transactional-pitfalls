package dev.perevalov.kafkapitfals.domain;

/**
 * Lifecycle states for outbox rows.
 */
public enum OutboxStatus {
    /**
     * Event is committed in DB but not yet published to Kafka.
     */
    NEW,
    /**
     * Event has been acknowledged by Kafka and marked by relay.
     */
    SENT
}
