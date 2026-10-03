package com.ashwini.router.gateway;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class GatewayState {

    private final String id;
    private final double failureRate;
    private final int latencyMs;

    private volatile double emaScore;
    private volatile boolean killed = false;

    private final AtomicInteger totalCalls = new AtomicInteger(0);
    private final AtomicInteger successCalls = new AtomicInteger(0);
    private final AtomicInteger failureCalls = new AtomicInteger(0);
    private final AtomicLong totalLatencyMs = new AtomicLong(0);

    public GatewayState(String id, double failureRate, int latencyMs,
                        double priorSuccess, double priorFailure) {
        this.id = id;
        this.failureRate = failureRate;
        this.latencyMs = latencyMs;
        this.emaScore = priorSuccess / (priorSuccess + priorFailure);
    }

    public String getId() { return id; }
    public double getFailureRate() { return failureRate; }
    public int getLatencyMs() { return latencyMs; }
    public double getEmaScore() { return emaScore; }
    public boolean isKilled() { return killed; }

    public int getTotalCalls() { return totalCalls.get(); }
    public int getSuccessCalls() { return successCalls.get(); }
    public int getFailureCalls() { return failureCalls.get(); }

    public double getAvgLatencyMs() {
        int calls = totalCalls.get();
        return calls == 0 ? latencyMs : (double) totalLatencyMs.get() / calls;
    }

    public synchronized void recordOutcome(boolean success, long latencyMs, double alpha) {
        totalCalls.incrementAndGet();
        if (success) successCalls.incrementAndGet();
        else failureCalls.incrementAndGet();
        totalLatencyMs.addAndGet(latencyMs);

        double outcome = success ? 1.0 : 0.0;
        this.emaScore = alpha * outcome + (1 - alpha) * this.emaScore;
    }

    public void kill() { this.killed = true; }
    public void revive() { this.killed = false; }
}