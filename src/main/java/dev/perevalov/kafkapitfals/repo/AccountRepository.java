package dev.perevalov.kafkapitfals.repo;

import dev.perevalov.kafkapitfals.domain.AccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository for account aggregate.
 */
public interface AccountRepository extends JpaRepository<AccountEntity, Long> {
    /**
     * Returns all accounts sorted by id for deterministic API output.
     *
     * @return sorted account list
     */
    List<AccountEntity> findAllByOrderByIdAsc();
}
