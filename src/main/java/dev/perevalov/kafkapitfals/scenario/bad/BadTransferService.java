package dev.perevalov.kafkapitfals.scenario.bad;

import dev.perevalov.kafkapitfals.domain.AccountEntity;
import dev.perevalov.kafkapitfals.domain.TransferEvent;
import dev.perevalov.kafkapitfals.repo.AccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Demonstrates the classic dual-write anti-pattern.
 *
 * <p>Problem: DB changes and Kafka publish are executed in one business method, but they are
 * coordinated by different transaction managers. A local DB rollback does not rollback a record
 * that has already been acknowledged by Kafka.</p>
 *
 * <p>Fix in this repository: this class is intentionally left unsafe as a baseline. Safer
 * alternatives are implemented in {@code OutboxTransferService} and
 * {@code IdempotentLedgerConsumer}.</p>
 */
@Service
public class BadTransferService {

    private final AccountRepository accountRepository;
    private final KafkaTemplate<String, TransferEvent> kafkaTemplate;
    private final String transferTopic;

    /**
     * Creates the service with dependencies required to read accounts and publish transfer events.
     *
     * @param accountRepository account storage used by the business transaction
     * @param kafkaTemplate producer used to publish events to Kafka
     * @param transferTopic topic name that receives transfer events
     */
    public BadTransferService(
            AccountRepository accountRepository,
            KafkaTemplate<String, TransferEvent> kafkaTemplate,
            @Value("${app.kafka.transfer-topic}") String transferTopic
    ) {
        this.accountRepository = accountRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.transferTopic = transferTopic;
    }

    /**
     * Executes transfer business logic and then publishes an event directly to Kafka.
     *
     * <p>Problem: if the method fails after Kafka acknowledge, the DB transaction rolls back while
     * the event remains visible to consumers. This produces a phantom event and inconsistent state
     * between DB and downstream services.</p>
     *
     * <p>Fix in improved scenarios: write to DB and outbox atomically in one DB transaction, then
     * publish from relay with idempotent consumer protection.</p>
     *
     * @param fromAccountId sender account id
     * @param toAccountId recipient account id
     * @param amount transfer amount represented as {@link Double}; kept here only to illustrate why
     *               money in floating-point is a bad idea
     * @param failAfterKafkaSend if {@code true}, simulates crash after Kafka publish and before method
     *                           completion
     * @return generated event id used as Kafka key and dedup identifier
     */
    @Transactional
    public String transferWithDualWrite(Long fromAccountId, Long toAccountId, Double amount, boolean failAfterKafkaSend) {
        AccountEntity from = loadAccount(fromAccountId);
        AccountEntity to = loadAccount(toAccountId);

        BigDecimal money = BigDecimal.valueOf(amount);
        from.debit(money);
        to.credit(money);

        String eventId = UUID.randomUUID().toString();
        TransferEvent event = new TransferEvent(eventId, fromAccountId, toAccountId, money, Instant.now());

        sendSynchronously(event);

        if (failAfterKafkaSend) {
            throw new IllegalStateException("Simulated crash after Kafka send. DB transaction will rollback.");
        }

        return eventId;
    }

    /**
     * Publishes the event and waits for broker acknowledgment.
     *
     * <p>Problem: synchronous waiting improves visibility of failures but still cannot make Kafka
     * and DB atomic together in this design.</p>
     *
     * @param event event to publish
     * @throws IllegalStateException when publish fails or acknowledge is not received in time
     */
    private void sendSynchronously(TransferEvent event) {
        try {
            kafkaTemplate.send(transferTopic, event.eventId(), event).get(10, TimeUnit.SECONDS);
        } catch (Exception ex) {
            throw new IllegalStateException("Kafka publish failed", ex);
        }
    }

    /**
     * Loads account by id.
     *
     * <p>Problem addressed: explicit validation avoids hidden {@code null} handling and fails fast
     * with a deterministic error.</p>
     *
     * @param id account id
     * @return existing account entity
     * @throws IllegalArgumentException when account is missing
     */
    private AccountEntity loadAccount(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("account not found: " + id));
    }
}
