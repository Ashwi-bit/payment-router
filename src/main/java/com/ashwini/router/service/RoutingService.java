package com.ashwini.router.service;

import com.ashwini.router.gateway.GatewayRegistry;
import com.ashwini.router.gateway.GatewayState;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * The brain of the router: picks the best gateway for a payment.
 * For now: EMA-based ranking. Fallback logic comes in Step 3.
 */
@Service
public class RoutingService {

    private final GatewayRegistry registry;

    public RoutingService(GatewayRegistry registry) {
        this.registry = registry;
    }

    /** Returns gateways ordered best-to-worst. The first is our top choice. */
    public List<GatewayState> rankedGateways() {
        return registry.rankedByEma();
    }

    /** Convenience: just the top pick. */
    public GatewayState pickBest() {
        List<GatewayState> ranked = rankedGateways();
        if (ranked.isEmpty()) {
            throw new IllegalStateException("No gateways configured");
        }
        return ranked.get(0);
    }
}