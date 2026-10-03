package com.ashwini.router.controller;

import com.ashwini.router.config.GatewaysProperties;

import com.ashwini.router.config.RouterProperties;
import com.ashwini.router.gateway.GatewayRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.ArrayList;
@RestController
public class HealthController {

    private final GatewaysProperties gatewayProps;
    private final RouterProperties routerProps;
    private final GatewayRegistry gatewayRegistry;
    private final JdbcTemplate jdbc;

    @Value("${gateways.list[0].id:NOT_FOUND}")
    private String firstGatewayId;

    @Value("${router.ema-alpha:NOT_FOUND}")
    private String emaAlphaFromYml;

    public HealthController(GatewaysProperties gatewayProps,
                            RouterProperties routerProps,
                            GatewayRegistry gatewayRegistry,
                            JdbcTemplate jdbc) {
        this.gatewayProps = gatewayProps;
        this.routerProps = routerProps;
        this.gatewayRegistry = gatewayRegistry;
        this.jdbc = jdbc;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("status", "ok");
        out.put("emaAlpha", routerProps.getEmaAlpha());
        out.put("maxAttempts", routerProps.getMaxAttempts());

        List<Map<String, Object>> gateways = gatewayProps.getList().stream()
            .map(g -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", g.getId());
                m.put("failureRate", g.getFailureRate());
                m.put("latencyMs", g.getLatencyMs());
                return m;
            })
            .collect(Collectors.toList());
        out.put("gateways", gateways);

        Integer tables = jdbc.queryForObject(
            "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME IN ('PAYMENTS','GATEWAY_EVENTS')",
            Integer.class);
        out.put("dbTablesReady", tables);

        return out;
    }

    @GetMapping("/debug")
    public Map<String, Object> debug() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("firstGatewayIdFromYml", firstGatewayId);
        m.put("emaAlphaFromYml", emaAlphaFromYml);
        m.put("gatewayListSize", gatewayProps.getList().size());
        return m;
    }

    @GetMapping("/debug/gateways")
    public Map<String, Object> debugGateways() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("count", gatewayRegistry.all().size());
        out.put("ranked", gatewayRegistry.snapshot());
        return out;
    }
    @GetMapping("/stats")
    public Map<String, Object> stats() {
        Map<String, Object> out = new LinkedHashMap<>();

        // Per-gateway stats from DB
        List<Map<String, Object>> dbStats = jdbc.queryForList(
            "SELECT gateway, " +
            "       COUNT(*) AS total, " +
            "       SUM(success) AS successes, " +
            "       AVG(latency_ms) AS avg_latency " +
            "FROM gateway_events GROUP BY gateway"
        );

        List<Map<String, Object>> enriched = new java.util.ArrayList<>();
        for (Map<String, Object> row : dbStats) {
            String gwId = (String) row.get("gateway");
            long total = ((Number) row.get("total")).longValue();
            long successes = row.get("successes") == null ? 0 : ((Number) row.get("successes")).longValue();
            double avgLatency = row.get("avg_latency") == null ? 0 : ((Number) row.get("avg_latency")).doubleValue();

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("gateway", gwId);
            m.put("totalCalls", total);
            m.put("successRate", total == 0 ? 0 : round((double) successes / total, 4));
            m.put("avgLatencyMs", round(avgLatency, 1));

            // live EMA from registry
            var state = gatewayRegistry.get(gwId);
            if (state != null) {
                m.put("liveEmaScore", round(state.getEmaScore(), 4));
            }
            enriched.add(m);
        }
        out.put("perGateway", enriched);

        // Overall stats
        Long totalPayments = jdbc.queryForObject(
            "SELECT COUNT(*) FROM payments", Long.class);
        Long successful = jdbc.queryForObject(
            "SELECT COUNT(*) FROM payments WHERE status = 'SUCCESS'", Long.class);
        Long failed = jdbc.queryForObject(
            "SELECT COUNT(*) FROM payments WHERE status = 'FAILED'", Long.class);

        Map<String, Object> overall = new LinkedHashMap<>();
        overall.put("totalPayments", totalPayments);
        overall.put("successful", successful);
        overall.put("failed", failed);
        overall.put("successRate", totalPayments == 0 ? 0 :
            round((double) successful / totalPayments, 4));
        out.put("overall", overall);

        return out;
    }

    private double round(double v, int digits) {
        double f = Math.pow(10, digits);
        return Math.round(v * f) / f;
    }
    
}