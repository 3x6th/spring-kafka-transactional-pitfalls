package dev.perevalov.kafkapitfals.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;

/**
 * Account aggregate used by transfer scenarios.
 *
 * <p>Fix over the initial anti-pattern code from screenshots: balance is represented with
 * {@link BigDecimal} instead of {@code double} to avoid precision drift in financial calculations.</p>
 */
@Entity
@Table(name = "accounts")
public class AccountEntity {

    @Id
    private Long id;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Column(nullable = false)
    private String status;

    @Version
    private Long version;

    /**
     * Protected constructor required by JPA.
     */
    protected AccountEntity() {
    }

    /**
     * Creates account with deterministic id, balance and status.
     *
     * @param id account id
     * @param balance initial balance
     * @param status business status
     */
    public AccountEntity(Long id, BigDecimal balance, String status) {
        this.id = id;
        this.balance = balance;
        this.status = status;
    }

    /**
     * Returns account id.
     *
     * @return account id
     */
    public Long getId() {
        return id;
    }

    /**
     * Returns current account balance.
     *
     * @return current balance
     */
    public BigDecimal getBalance() {
        return balance;
    }

    /**
     * Returns account status.
     *
     * @return status value
     */
    public String getStatus() {
        return status;
    }

    /**
     * Returns optimistic lock version.
     *
     * @return entity version
     */
    public Long getVersion() {
        return version;
    }

    /**
     * Subtracts amount from balance.
     *
     * <p>Problem addressed: prevents negative balance by validating funds before subtraction.</p>
     *
     * @param amount amount to subtract
     * @throws IllegalArgumentException when funds are insufficient
     */
    public void debit(BigDecimal amount) {
        if (balance.compareTo(amount) < 0) {
            throw new IllegalArgumentException("insufficient funds");
        }
        balance = balance.subtract(amount);
    }

    /**
     * Adds amount to balance.
     *
     * @param amount amount to add
     */
    public void credit(BigDecimal amount) {
        balance = balance.add(amount);
    }
}
