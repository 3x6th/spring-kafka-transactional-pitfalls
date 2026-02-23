package dev.perevalov.kafkapitfals.repo;

import dev.perevalov.kafkapitfals.domain.NaiveLedgerEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for rows produced by non-idempotent consumer.
 */
public interface NaiveLedgerEntryRepository extends JpaRepository<NaiveLedgerEntryEntity, Long> {
    /**
     * Counts rows with the same event id.
     *
     * <p>Used to demonstrate duplicate side effects under retry/replay.</p>
     *
     * @param eventId event identifier
     * @return number of rows for this event id
     */
    long countByEventId(String eventId);
}
