package com.ashwini.router.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PaymentRequest {

    @Min(value = 1, message = "amount must be >= 1")
    private long amount;                    // amount in paise (integer)

    @NotBlank(message = "currency is required")
    @Size(min = 3, max = 3, message = "currency must be 3 letters")
    private String currency;                // e.g. "INR"

    @NotBlank(message = "idempotencyKey is required")
    @Size(min = 8, max = 128, message = "idempotencyKey must be 8-128 chars")
    private String idempotencyKey;

    public long getAmount() { return amount; }
    public void setAmount(long amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
}