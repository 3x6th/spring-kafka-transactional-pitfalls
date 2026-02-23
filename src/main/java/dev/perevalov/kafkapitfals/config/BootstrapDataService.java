package dev.perevalov.kafkapitfals.config;

import dev.perevalov.kafkapitfals.domain.AccountEntity;
import dev.perevalov.kafkapitfals.repo.AccountRepository;
import dev.perevalov.kafkapitfals.repo.IdempotentLedgerEntryRepository;
import dev.perevalov.kafkapitfals.repo.NaiveLedgerEntryRepository;
import dev.perevalov.kafkapitfals.repo.OutboxEventRepository;
import dev.perevalov.kafkapitfals.repo.ProcessedEventRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Provides deterministic seed/reset routines for local labs and integration tests.
 *
 * <p>Problem addressed: without deterministic baseline, account ids and balances drift between runs,
 * making scenario instructions and assertions unstable.</p>
 */
@Service
public class BootstrapDataService {

    private final AccountRepository accountRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final NaiveLedgerEntryRepository naiveLedgerEntryRepository;
    private final IdempotentLedgerEntryRepository idempotentLedgerEntryRepository;
    private final ProcessedEventRepository processedEventRepository;

    /**
     * Creates bootstrap service dependencies.
     *
     * @param accountRepository account storage
     * @param outboxEventRepository outbox storage
     * @param naiveLedgerEntryRepository naive consumer side-effect storage
     * @param idempotentLedgerEntryRepository idempotent consumer side-effect storage
     * @param processedEventRepository dedup marker storage
     */
    public BootstrapDataService(
            AccountRepository accountRepository,
            OutboxEventRepository outboxEventRepository,
            NaiveLedgerEntryRepository naiveLedgerEntryRepository,
            IdempotentLedgerEntryRepository idempotentLedgerEntryRepository,
            ProcessedEventRepository processedEventRepository
    ) {
        this.accountRepository = accountRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.naiveLedgerEntryRepository = naiveLedgerEntryRepository;
        this.idempotentLedgerEntryRepository = idempotentLedgerEntryRepository;
        this.processedEventRepository = processedEventRepository;
    }

    /**
     * Inserts baseline accounts on application startup when storage is empty.
     *
     * @implNote Running this in one transaction guarantees either full seed or no seed.
     */
    @PostConstruct
    @Transactional
    public void initIfEmpty() {
        if (accountRepository.count() == 0) {
            accountRepository.saveAll(defaultAccounts());
        }
    }

    /**
     * Removes all experiment data and restores baseline accounts.
     *
     * <p>Fix for reproducibility: reset order clears dependent tables first, then recreates accounts
     * with stable ids expected by API examples and tests.</p>
     */
    @Transactional
    public void resetAll() {
        naiveLedgerEntryRepository.deleteAllInBatch();
        idempotentLedgerEntryRepository.deleteAllInBatch();
        processedEventRepository.deleteAllInBatch();
        outboxEventRepository.deleteAllInBatch();
        accountRepository.deleteAllInBatch();
        accountRepository.saveAll(defaultAccounts());
    }

    /**
     * Builds default account set with fixed ids and balances.
     *
     * @return deterministic account baseline
     */
    private List<AccountEntity> defaultAccounts() {
        return List.of(
                new AccountEntity(1L, new BigDecimal("1000.00"), "ACTIVE"),
                new AccountEntity(2L, new BigDecimal("500.00"), "ACTIVE"),
                new AccountEntity(3L, new BigDecimal("250.00"), "ACTIVE")
        );
    }
}
