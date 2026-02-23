package dev.perevalov.kafkapitfals.api;

import dev.perevalov.kafkapitfals.scenario.outbox.OutboxRelayService;
import dev.perevalov.kafkapitfals.scenario.outbox.OutboxTransferService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST entry point for outbox-based producer flow.
 *
 * <p>Fix over bad scenario: business state and integration event are persisted atomically in DB.
 * Kafka publish is performed separately by relay endpoint.</p>
 */
@RestController
@RequestMapping("/api/outbox")
public class OutboxScenarioController {

    private final OutboxTransferService outboxTransferService;
    private final OutboxRelayService outboxRelayService;

    /**
     * Creates controller for outbox transfer and relay operations.
     *
     * @param outboxTransferService service that writes business state and outbox rows atomically
     * @param outboxRelayService service that publishes pending outbox events to Kafka
     */
    public OutboxScenarioController(
            OutboxTransferService outboxTransferService,
            OutboxRelayService outboxRelayService
    ) {
        this.outboxTransferService = outboxTransferService;
        this.outboxRelayService = outboxRelayService;
    }

    /**
     * Creates business transfer and persists integration event into outbox.
     *
     * @param request transfer request with optional failure simulation before DB commit
     * @return event metadata for later relay
     */
    @PostMapping("/transfer")
    public TransferResponse transfer(@Valid @RequestBody OutboxTransferRequest request) {
        String eventId = outboxTransferService.transferWithOutbox(
                request.fromAccountId(),
                request.toAccountId(),
                request.amount(),
                request.failAfterDbWrite()
        );
        return new TransferResponse(eventId, "OUTBOX event stored");
    }

    /**
     * Runs outbox relay manually for laboratory control.
     *
     * <p>Problem demonstrated: optional failure after first successful send but before outbox status
     * update shows why duplicates are possible and why idempotent consumer is required.</p>
     *
     * @param batchSize maximum number of pending outbox rows
     * @param failAfterFirstSend enables crash simulation window
     * @return relay counters for selected and sent events
     */
    @PostMapping("/relay")
    public OutboxRelayService.RelayResult relay(
            @RequestParam(defaultValue = "50") int batchSize,
            @RequestParam(defaultValue = "false") boolean failAfterFirstSend
    ) {
        return outboxRelayService.relayPending(batchSize, failAfterFirstSend);
    }
}
