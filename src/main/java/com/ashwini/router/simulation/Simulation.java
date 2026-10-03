package com.ashwini.router.simulation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Standalone simulation comparing:
 *   1. Random routing (pick a random gateway, no retries)
 *   2. Smart routing (EMA-based selection + fallback across 3 gateways)
 *
 * Runs N payments for each strategy and prints success rates.
 * Does NOT use Spring — pure logic for speed.
 */
public class Simulation {

    // === Configuration ===
    private static final int N = 1000;
    private static final int MAX_ATTEMPTS = 3;
    private static final double EMA_ALPHA = 0.3;
    private static final double PRIOR_SUCCESS = 5;
    private static final double PRIOR_FAILURE = 5;

    // Gateway configurations (matches application.yml)
    private static final String[] GW_IDS   = {"gw-a", "gw-b", "gw-c"};
    private static final double[] GW_FAIL  = {0.10,  0.30,  0.50};
    private static final int[] GW_LATENCY  = {80, 150, 250};

    // === Runtime state ===
    private static final double[] emaScores = new double[GW_IDS.length];
    private static final int[] gatewayUsage = new int[GW_IDS.length];

    private static final Random rng = new Random(42);  // fixed seed = reproducible

    public static void main(String[] args) {
        System.out.println("=== Payment Router Simulation ===");
        System.out.println("N = " + N + " payments per strategy");
        System.out.println("Gateways: gw-a (10% fail), gw-b (30% fail), gw-c (50% fail)");
        System.out.println();

        // --- Run 1: Random routing (control) ---
        resetEma();
        Result random = runRandomRouting();

        // --- Run 2: Smart routing (EMA + fallback) ---
        resetEma();
        Result smart = runSmartRouting();

        // --- Report ---
        System.out.println("=== Results ===");
        System.out.printf("%-20s %-12s %-12s %-12s%n",
            "Strategy", "Success", "Success%", "Total Attempts");
        System.out.println("-".repeat(60));
        System.out.printf("%-20s %-12d %-11.1f%% %-12d%n",
            "Random routing",
            random.successes,
            100.0 * random.successes / N,
            random.totalAttempts);
        System.out.printf("%-20s %-12d %-11.1f%% %-12d%n",
            "Smart routing",
            smart.successes,
            100.0 * smart.successes / N,
            smart.totalAttempts);

        double improvement =
            (100.0 * smart.successes / N) - (100.0 * random.successes / N);

        System.out.println();
        System.out.printf("Improvement: +%.1f percentage points%n", improvement);
        System.out.printf("Extra attempts (fallbacks): %d%n",
            smart.totalAttempts - N);

        // === Per-gateway usage during smart routing ===
        System.out.println();
        System.out.println("Gateway usage during smart routing:");
        for (int i = 0; i < GW_IDS.length; i++) {
            System.out.printf("  %s: %d calls (%.1f%%)%n",
                GW_IDS[i],
                gatewayUsage[i],
                100.0 * gatewayUsage[i] / smart.totalAttempts);
        }

        // === Final EMA scores vs true success rates ===
        System.out.println();
        System.out.println("Final EMA scores after smart routing:");
        for (int i = 0; i < GW_IDS.length; i++) {
            System.out.printf("  %s: %.4f (true success rate: %.2f)%n",
                GW_IDS[i],
                emaScores[i],
                1.0 - GW_FAIL[i]);
        }

        // === Control test: all gateways equal ===
        runEqualGatewaysTest();
    }

    // ---------- Random routing: pick uniformly, no retry ----------
    private static Result runRandomRouting() {
        int successes = 0;
        int totalAttempts = 0;

        for (int i = 0; i < N; i++) {
            int gwIdx = rng.nextInt(GW_IDS.length);
            totalAttempts++;
            if (callGateway(gwIdx)) successes++;
        }

        return new Result(successes, totalAttempts);
    }

    // ---------- Smart routing: best EMA first, fallback on failure ----------
    private static Result runSmartRouting() {
        int successes = 0;
        int totalAttempts = 0;

        for (int i = 0; i < N; i++) {
            List<Integer> ranked = rankByEma();

            boolean paid = false;
            for (int attempt = 0;
                 attempt < MAX_ATTEMPTS && attempt < ranked.size();
                 attempt++) {

                int gwIdx = ranked.get(attempt);
                totalAttempts++;

                boolean success = callGateway(gwIdx);
                updateEma(gwIdx, success);

                if (success) {
                    paid = true;
                    break;
                }
            }

            if (paid) successes++;
        }

        return new Result(successes, totalAttempts);
    }

    /** Simulate a call to gateway at index. Returns true if success. */
    private static boolean callGateway(int gwIdx) {
        gatewayUsage[gwIdx]++;
        return rng.nextDouble() >= GW_FAIL[gwIdx];
    }

    /** Update EMA after an outcome. */
    private static void updateEma(int gwIdx, boolean success) {
        double outcome = success ? 1.0 : 0.0;
        emaScores[gwIdx] = EMA_ALPHA * outcome + (1 - EMA_ALPHA) * emaScores[gwIdx];
    }

    /** Return gateway indices sorted by EMA, highest first. */
    private static List<Integer> rankByEma() {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < GW_IDS.length; i++) indices.add(i);

        indices.sort(
            Comparator.comparingDouble((Integer i) -> emaScores[i]).reversed()
        );
        return indices;
    }

    /** Reset all EMAs to the cold-start prior and clear usage counters. */
    private static void resetEma() {
        double prior = PRIOR_SUCCESS / (PRIOR_SUCCESS + PRIOR_FAILURE);
        for (int i = 0; i < emaScores.length; i++) emaScores[i] = prior;
        for (int i = 0; i < gatewayUsage.length; i++) gatewayUsage[i] = 0;
    }

    /**
     * Control test: set all gateways to the same failure rate (30%).
     * If smart routing still looks great, the improvement is from retries
     * alone, not from smart choice. Expect a much smaller gain here.
     */
    private static void runEqualGatewaysTest() {
        System.out.println();
        System.out.println("=== Control Test: All gateways equal (30% fail) ===");

        double[] original = GW_FAIL.clone();
        for (int i = 0; i < GW_FAIL.length; i++) GW_FAIL[i] = 0.30;

        resetEma();
        Result smartEqual = runSmartRouting();

        System.out.printf("Smart routing with equal gateways: %.1f%% success%n",
            100.0 * smartEqual.successes / N);
        System.out.println("(Should be much closer to random than the uneven case)");

        // restore original failure rates
        System.arraycopy(original, 0, GW_FAIL, 0, GW_FAIL.length);
    }

    /** Result holder. */
    private static class Result {
        final int successes;
        final int totalAttempts;

        Result(int successes, int totalAttempts) {
            this.successes = successes;
            this.totalAttempts = totalAttempts;
        }
    }
}