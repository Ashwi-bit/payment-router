package com.ashwini.router.model;

import java.time.Instant;
import java.util.List;

public class PaymentResponse {

    private String idempotencyKey;
    private String status;              // SUCCESS | FAILED
    private String gateway;             // final gateway used (or last tried)
    private int attempts;               // how many gateways we tried
    private List<String> attemptedGateways;
    private String transactionId;       // fake ID from mock gateway
    private String message;             // human-readable
    private Instant createdAt;
    private boolean duplicate;          // true if this was a replay of a previous request

    // getters and setters...

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getGateway() { return gateway; }
    public void setGateway(String gateway) { this.gateway = gateway; }

    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }

    public List<String> getAttemptedGateways() { return attemptedGateways; }
    public void setAttemptedGateways(List<String> attemptedGateways) { this.attemptedGateways = attemptedGateways; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public boolean isDuplicate() { return duplicate; }
    public void setDuplicate(boolean duplicate) { this.duplicate = duplicate; }
}