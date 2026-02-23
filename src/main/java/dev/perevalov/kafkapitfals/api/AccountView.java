package dev.perevalov.kafkapitfals.api;

import java.math.BigDecimal;

/**
 * Read model for account state visible in laboratory API.
 *
 * @param id account id
 * @param balance current account balance
 * @param status business status flag
 */
public record AccountView(
        Long id,
        BigDecimal balance,
        String status
) {
}
