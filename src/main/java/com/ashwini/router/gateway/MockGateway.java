package com.ashwini.router.gateway;

import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.UUID;

@Component
public class MockGateway {

    private final Random random = new Random();

    /**
     * Simulates processing a payment.
     * Returns a GatewayCallResult with success flag, latency, and (on success) a fake txn ID.
     */
    public GatewayCallResult process(GatewayState state, long amount) {
        long start = System.currentTimeMillis();
        long configuredLatency = state.getLatencyMs();

        try {
            Thread.sleep(configuredLatency);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            long elapsed = System.currentTimeMillis() - start;
            return new GatewayCallResult(false, elapsed, null, "interrupted");
        }

        long elapsed = System.currentTimeMillis() - start;
        boolean success = random.nextDouble() >= state.getFailureRate();

        if (success) {
            return new GatewayCallResult(true, elapsed, "txn_" + UUID.randomUUID(), null);
        } else {
            return new GatewayCallResult(false, elapsed, null, "gateway_declined");
        }
    }

    public static class GatewayCallResult {
        private final boolean success;
        private final long latencyMs;
        private final String transactionId;
        private final String error;

        public GatewayCallResult(boolean success, long latencyMs, String transactionId, String error) {
            this.success = success;
            this.latencyMs = latencyMs;
            this.transactionId = transactionId;
            this.error = error;
        }

        public boolean isSuccess() { return success; }
        public long getLatencyMs() { return latencyMs; }
        public String getTransactionId() { return transactionId; }
        public String getError() { return error; }
    }
}