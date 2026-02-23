package dev.perevalov.kafkapitfals.scenario.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.perevalov.kafkapitfals.domain.AccountEntity;
import dev.perevalov.kafkapitfals.domain.OutboxEventEntity;
import dev.perevalov.kafkapitfals.domain.OutboxStatus;
import dev.perevalov.kafkapitfals.domain.TransferEvent;
import dev.perevalov.kafkapitfals.repo.AccountRepository;
import dev.perevalov.kafkapitfals.repo.OutboxEventRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Producer-side implementation of the transactional outbox pattern.
 *
 * <p>Problem addressed: direct DB + Kafka dual write can leave system in inconsistent state when one
 * side succeeds and the other fails.</p>
 *
 * <p>Fix: business state update and outbox row creation are committed atomically in one DB
 * transaction. Kafka publish is moved to a separate relay process that reads pending outbox rows.</p>
 */
@Service
public class OutboxTransferService {

    private final AccountRepository accountRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final String transferTopic;

    /**
     * Creates service dependencies for account updates and outbox writes.
     *
     * @param accountRepository account storage for debit/credit operations
     * @param outboxEventRepository outbox storage for reliable event handoff
     * @param objectMapper serializer used to store event payload in JSON form
     * @param transferTopic Kafka topic to be used later by relay
     */
    public OutboxTransferService(
            AccountRepository accountRepository,
            OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper,
            @Value("${app.kafka.transfer-topic}") String transferTopic
    ) {
        this.accountRepository = accountRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
        this.transferTopic = transferTopic;
    }

    /**
     * Applies account transfer and writes outbox event in the same DB transaction.
     *
     * <p>Problem addressed: without outbox, producer cannot guarantee atomicity between business data
     * and emitted event.</p>
     *
     * <p>Fix: this method writes both state and integration event in one local transaction. Either
     * both are committed or both are rolled back.</p>
     *
     * @param fromAccountId sender account id
     * @param toAccountId recipient account id
     * @param amount transfer amount as {@link BigDecimal} to avoid floating-point rounding issues
     * @param failAfterDbWrite test hook that simulates crash before commit
     * @return generated event id
     */
    @Transactional
    public String transferWithOutbox(Long fromAccountId, Long toAccountId, BigDecimal amount, boolean failAfterDbWrite) {
        AccountEntity from = loadAccount(fromAccountId);
        AccountEntity to = loadAccount(toAccountId);

        from.debit(amount);
        to.credit(amount);

        String eventId = UUID.randomUUID().toString();
        TransferEvent event = new TransferEvent(eventId, fromAccountId, toAccountId, amount, Instant.now());

        outboxEventRepository.save(new OutboxEventEntity(
                eventId,
                transferTopic,
                eventId,
                toJson(event),
                OutboxStatus.NEW,
                Instant.now()
        ));

        if (failAfterDbWrite) {
            throw new IllegalStateException("Simulated crash before DB commit. Nothing should be committed.");
        }

        return eventId;
    }

    /**
     * Serializes event payload for durable outbox storage.
     *
     * <p>Problem addressed: relay needs stable payload representation independent of in-memory state.</p>
     *
     * @param event transfer event to serialize
     * @return JSON payload stored in outbox table
     * @throws IllegalStateException when serialization fails
     */
    private String toJson(TransferEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Cannot serialize event", ex);
        }
    }

    /**
     * Loads account by id and fails fast if it does not exist.
     *
     * @param id account id
     * @return existing account
     * @throws IllegalArgumentException when account is absent
     */
    private AccountEntity loadAccount(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("account not found: " + id));
    }
}
