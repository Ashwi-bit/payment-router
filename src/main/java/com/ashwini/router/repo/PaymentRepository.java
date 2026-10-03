package com.ashwini.router.repo;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

@Repository
public class PaymentRepository {

    private final JdbcTemplate jdbc;

    public PaymentRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Try to claim the idempotency key by inserting a PENDING row.
     * Returns:
     *   - empty Optional → this is a NEW request (we own the key)
     *   - present Optional → DUPLICATE (key already exists; return existing row)
     */
    public Optional<PaymentRow> tryClaim(String key, long amount, String currency) {
        // Fast path: check if it already exists
        Optional<PaymentRow> existing = findByKey(key);
        if (existing.isPresent()) {
            return existing;
        }

        // Try to insert. If a concurrent insert happens, we catch and refetch.
        try {
            jdbc.update(
                "INSERT INTO payments (idempotency_key, amount, currency, status, attempts) " +
                "VALUES (?, ?, ?, 'PENDING', 0)",
                key, amount, currency
            );
            return Optional.empty();
        } catch (DuplicateKeyException e) {
            // race condition: someone else inserted first
            return findByKey(key);
        }
    }

    public Optional<PaymentRow> findByKey(String key) {
        return jdbc.query(
            "SELECT * FROM payments WHERE idempotency_key = ?",
            new Object[]{key},
            new PaymentRowMapper()
        ).stream().findFirst();
    }

    public void finalizePayment(String key, String status, String gateway,
                                int attempts, String responseBody) {
        jdbc.update(
            "UPDATE payments SET status = ?, gateway = ?, attempts = ?, " +
            "response_body = ?, updated_at = CURRENT_TIMESTAMP WHERE idempotency_key = ?",
            status, gateway, attempts, responseBody, key
        );
    }

    public static class PaymentRow {
        private final String idempotencyKey;
        private final long amount;
        private final String currency;
        private final String status;
        private final String gateway;
        private final int attempts;
        private final String responseBody;
        private final Instant createdAt;

        public PaymentRow(String idempotencyKey, long amount, String currency, String status,
                          String gateway, int attempts, String responseBody, Instant createdAt) {
            this.idempotencyKey = idempotencyKey;
            this.amount = amount;
            this.currency = currency;
            this.status = status;
            this.gateway = gateway;
            this.attempts = attempts;
            this.responseBody = responseBody;
            this.createdAt = createdAt;
        }

        public String getIdempotencyKey() { return idempotencyKey; }
        public long getAmount() { return amount; }
        public String getCurrency() { return currency; }
        public String getStatus() { return status; }
        public String getGateway() { return gateway; }
        public int getAttempts() { return attempts; }
        public String getResponseBody() { return responseBody; }
        public Instant getCreatedAt() { return createdAt; }
    }

    private static class PaymentRowMapper implements RowMapper<PaymentRow> {
        @Override
        public PaymentRow mapRow(ResultSet rs, int rowNum) throws SQLException {
            Timestamp created = rs.getTimestamp("created_at");
            return new PaymentRow(
                rs.getString("idempotency_key"),
                rs.getLong("amount"),
                rs.getString("currency"),
                rs.getString("status"),
                rs.getString("gateway"),
                rs.getInt("attempts"),
                rs.getString("response_body"),
                created == null ? null : created.toInstant()
            );
        }
    }
}