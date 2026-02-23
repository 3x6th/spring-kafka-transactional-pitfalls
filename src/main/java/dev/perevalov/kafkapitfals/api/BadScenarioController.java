package dev.perevalov.kafkapitfals.api;

import dev.perevalov.kafkapitfals.scenario.bad.BadTransferService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST entry point for the unsafe dual-write scenario.
 *
 * <p>Problem demonstrated via API: producer updates DB and publishes to Kafka directly, so rollback
 * of DB transaction does not guarantee rollback of published event.</p>
 */
@RestController
@RequestMapping("/api/bad")
public class BadScenarioController {

    private final BadTransferService badTransferService;

    /**
     * Creates controller for triggering bad scenario transfers.
     *
     * @param badTransferService service that implements unsafe dual-write flow
     */
    public BadScenarioController(BadTransferService badTransferService) {
        this.badTransferService = badTransferService;
    }

    /**
     * Executes unsafe transfer operation.
     *
     * <p>Problem: request can intentionally fail after Kafka send to show phantom event behavior.</p>
     *
     * @param request transfer request with simulation flags
     * @return event metadata for inspection
     */
    @PostMapping("/transfer")
    public TransferResponse transfer(@Valid @RequestBody BadTransferRequest request) {
        String eventId = badTransferService.transferWithDualWrite(
                request.fromAccountId(),
                request.toAccountId(),
                request.amount(),
                request.failAfterKafkaSend()
        );

        return new TransferResponse(eventId, "BAD scenario executed");
    }
}
