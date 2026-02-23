package dev.perevalov.kafkapitfals.api;

/**
 * Common response for transfer-triggering endpoints.
 *
 * @param eventId generated event identifier
 * @param note short human-readable execution note
 */
public record TransferResponse(
        String eventId,
        String note
) {
}
