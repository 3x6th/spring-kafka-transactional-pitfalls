package dev.perevalov.kafkapitfals.repo;

import dev.perevalov.kafkapitfals.domain.IdempotentLedgerEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for rows produced by idempotent consumer.
 */
public interface IdempotentLedgerEntryRepository extends JpaRepository<IdempotentLedgerEntryEntity, Long> {
    /**
     * Counts rows with the same event id.
     *
     * <p>Expected value is one for correctly deduplicated flow.</p>
     *
     * @param eventId event identifier
     * @return number of rows for this event id
     */
    long countByEventId(String eventId);
}
