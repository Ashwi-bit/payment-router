package com.ashwini.router.gateway;

import com.ashwini.router.config.GatewaysProperties;
import com.ashwini.router.config.RouterProperties;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class GatewayRegistry {

    private final GatewaysProperties gatewayProps;
    private final RouterProperties routerProps;
    private final Map<String, GatewayState> states = new ConcurrentHashMap<>();

    public GatewayRegistry(GatewaysProperties gatewayProps, RouterProperties routerProps) {
        this.gatewayProps = gatewayProps;
        this.routerProps = routerProps;
    }

    @PostConstruct
    public void init() {
        for (GatewaysProperties.GatewayConfig cfg : gatewayProps.getList()) {
            GatewayState state = new GatewayState(
                cfg.getId(),
                cfg.getFailureRate(),
                cfg.getLatencyMs(),
                routerProps.getPriorSuccess(),
                routerProps.getPriorFailure()
            );
            states.put(cfg.getId(), state);
        }
        System.out.println("[GatewayRegistry] Loaded " + states.size() + " gateways: " + states.keySet());
    }

    public GatewayState get(String id) {
        return states.get(id);
    }

    public List<GatewayState> all() {
        return new ArrayList<>(states.values());
    }

    public List<GatewayState> allIncludingKilled() {
        return new ArrayList<>(states.values());
    }

    /** Returns non-killed gateways sorted by EMA score, highest first. */
    public List<GatewayState> rankedByEma() {
        return states.values().stream()
            .filter(s -> !s.isKilled())
            .sorted(Comparator.comparingDouble(GatewayState::getEmaScore).reversed())
            .collect(Collectors.toList());
    }

    /** Snapshot for /debug/gateways (only alive gateways). */
    public List<Map<String, Object>> snapshot() {
        return rankedByEma().stream().map(s -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", s.getId());
            m.put("emaScore", round(s.getEmaScore(), 4));
            m.put("totalCalls", s.getTotalCalls());
            m.put("success", s.getSuccessCalls());
            m.put("failure", s.getFailureCalls());
            m.put("avgLatencyMs", round(s.getAvgLatencyMs(), 1));
            m.put("configuredFailureRate", s.getFailureRate());
            return m;
        }).collect(Collectors.toList());
    }

    /** Snapshot including killed flag — used by /gateways. */
    public List<Map<String, Object>> snapshotAll() {
        return states.values().stream()
            .sorted(Comparator.comparingDouble(GatewayState::getEmaScore).reversed())
            .map(s -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", s.getId());
                m.put("emaScore", round(s.getEmaScore(), 4));
                m.put("killed", s.isKilled());
                m.put("totalCalls", s.getTotalCalls());
                m.put("success", s.getSuccessCalls());
                m.put("failure", s.getFailureCalls());
                m.put("avgLatencyMs", round(s.getAvgLatencyMs(), 1));
                m.put("configuredFailureRate", s.getFailureRate());
                return m;
            }).collect(Collectors.toList());
    }

    public boolean kill(String id) {
        GatewayState s = states.get(id);
        if (s == null) return false;
        s.kill();
        return true;
    }

    public boolean revive(String id) {
        GatewayState s = states.get(id);
        if (s == null) return false;
        s.revive();
        return true;
    }

    private double round(double v, int digits) {
        double factor = Math.pow(10, digits);
        return Math.round(v * factor) / factor;
    }
}