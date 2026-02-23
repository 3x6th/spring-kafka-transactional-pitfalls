package dev.perevalov.kafkapitfals.repo;

import dev.perevalov.kafkapitfals.domain.ProcessedEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for consumer dedup markers.
 */
public interface ProcessedEventRepository extends JpaRepository<ProcessedEventEntity, String> {
}
