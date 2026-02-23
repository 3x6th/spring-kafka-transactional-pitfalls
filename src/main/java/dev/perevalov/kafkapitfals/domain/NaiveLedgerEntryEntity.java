package dev.perevalov.kafkapitfals.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Side-effect row created by naive consumer.
 *
 * <p>Problem demonstrated: duplicate deliveries create duplicate rows with the same event id.</p>
 */
@Entity
@Table(name = "naive_ledger_entries")
public class NaiveLedgerEntryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String eventId;

    @Column(nullable = false)
    private Long accountId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal delta;

    @Column(nullable = false)
    private Instant createdAt;

    /**
     * Protected constructor required by JPA.
     */
    protected NaiveLedgerEntryEntity() {
    }

    /**
     * Creates ledger row for one consumed event.
     *
     * @param eventId consumed event id
     * @param accountId affected account id
     * @param delta applied amount
     * @param createdAt insertion timestamp
     */
    public NaiveLedgerEntryEntity(String eventId, Long accountId, BigDecimal delta, Instant createdAt) {
        this.eventId = eventId;
        this.accountId = accountId;
        this.delta = delta;
        this.createdAt = createdAt;
    }

    /**
     * Returns row id.
     *
     * @return row id
     */
    public Long getId() {
        return id;
    }

    /**
     * Returns event id.
     *
     * @return event id
     */
    public String getEventId() {
        return eventId;
    }

    /**
     * Returns affected account id.
     *
     * @return account id
     */
    public Long getAccountId() {
        return accountId;
    }

    /**
     * Returns applied amount.
     *
     * @return delta amount
     */
    public BigDecimal getDelta() {
        return delta;
    }

    /**
     * Returns creation timestamp.
     *
     * @return creation instant
     */
    public Instant getCreatedAt() {
        return createdAt;
    }
}
