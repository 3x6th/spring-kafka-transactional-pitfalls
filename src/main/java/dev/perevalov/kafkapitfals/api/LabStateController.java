package dev.perevalov.kafkapitfals.api;

import dev.perevalov.kafkapitfals.config.BootstrapDataService;
import dev.perevalov.kafkapitfals.domain.OutboxStatus;
import dev.perevalov.kafkapitfals.repo.AccountRepository;
import dev.perevalov.kafkapitfals.repo.IdempotentLedgerEntryRepository;
import dev.perevalov.kafkapitfals.repo.NaiveLedgerEntryRepository;
import dev.perevalov.kafkapitfals.repo.OutboxEventRepository;
import dev.perevalov.kafkapitfals.repo.ProcessedEventRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Collectors;

/**
 * Exposes current laboratory state for manual verification.
 *
 * <p>Purpose: helps compare bad and fixed scenarios by returning account balances, outbox queue
 * status and consumer-side counters in one response.</p>
 */
@RestController
@RequestMapping("/api/state")
public class LabStateController {

    private final BootstrapDataService bootstrapDataService;
    private final AccountRepository accountRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final NaiveLedgerEntryRepository naiveLedgerEntryRepository;
    private final IdempotentLedgerEntryRepository idempotentLedgerEntryRepository;
    private final ProcessedEventRepository processedEventRepository;

    /**
     * Creates state controller dependencies.
     *
     * @param bootstrapDataService reset helper used to restore deterministic baseline
     * @param accountRepository source of account balances
     * @param outboxEventRepository source of outbox states
     * @param naiveLedgerEntryRepository source of naive consumer side effects
     * @param idempotentLedgerEntryRepository source of idempotent consumer side effects
     * @param processedEventRepository source of dedup markers
     */
    public LabStateController(
            BootstrapDataService bootstrapDataService,
            AccountRepository accountRepository,
            OutboxEventRepository outboxEventRepository,
            NaiveLedgerEntryRepository naiveLedgerEntryRepository,
            IdempotentLedgerEntryRepository idempotentLedgerEntryRepository,
            ProcessedEventRepository processedEventRepository
    ) {
        this.bootstrapDataService = bootstrapDataService;
        this.accountRepository = accountRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.naiveLedgerEntryRepository = naiveLedgerEntryRepository;
        this.idempotentLedgerEntryRepository = idempotentLedgerEntryRepository;
        this.processedEventRepository = processedEventRepository;
    }

    /**
     * Returns current state snapshot.
     *
     * @return aggregated laboratory state
     */
    @GetMapping
    public LabStateResponse state() {
        return buildState();
    }

    /**
     * Resets all tables used by experiments and returns fresh snapshot.
     *
     * <p>Fix for reproducibility: deterministic reset prevents flaky manual scenarios and unstable
     * test baselines.</p>
     *
     * @return state after reset
     */
    @PostMapping("/reset")
    public LabStateResponse reset() {
        bootstrapDataService.resetAll();
        return buildState();
    }

    /**
     * Collects values from all stores into one DTO.
     *
     * @return structured laboratory state used by API and tests
     */
    private LabStateResponse buildState() {
        return new LabStateResponse(
                accountRepository.findAllByOrderByIdAsc()
                        .stream()
                        .map(a -> new AccountView(a.getId(), a.getBalance(), a.getStatus()))
                        .collect(Collectors.toList()),
                outboxEventRepository.findAllByOrderByCreatedAtAsc()
                        .stream()
                        .map(o -> new OutboxEventView(o.getId(), o.getStatus(), o.getCreatedAt(), o.getSentAt()))
                        .collect(Collectors.toList()),
                outboxEventRepository.countByStatus(OutboxStatus.NEW),
                outboxEventRepository.countByStatus(OutboxStatus.SENT),
                naiveLedgerEntryRepository.count(),
                idempotentLedgerEntryRepository.count(),
                processedEventRepository.count()
        );
    }
}
