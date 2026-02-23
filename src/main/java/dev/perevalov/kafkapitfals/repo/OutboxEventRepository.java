package dev.perevalov.kafkapitfals.repo;

import dev.perevalov.kafkapitfals.domain.OutboxEventEntity;
import dev.perevalov.kafkapitfals.domain.OutboxStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository for transactional outbox rows.
 */
public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, String> {
    /**
     * Returns pending rows ordered by creation time and limited by page size.
     *
     * @param status desired outbox status
     * @param pageable page request that limits batch size
     * @return ordered batch for relay
     */
    List<OutboxEventEntity> findByStatusOrderByCreatedAtAsc(OutboxStatus status, Pageable pageable);

    /**
     * Returns all rows ordered by creation time for API inspection.
     *
     * @return ordered outbox list
     */
    List<OutboxEventEntity> findAllByOrderByCreatedAtAsc();

    /**
     * Counts rows by status.
     *
     * @param status status to count
     * @return number of rows in the status
     */
    long countByStatus(OutboxStatus status);
}
