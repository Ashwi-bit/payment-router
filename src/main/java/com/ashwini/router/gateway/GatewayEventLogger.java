package com.ashwini.router.gateway;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class GatewayEventLogger {

    private final JdbcTemplate jdbc;

    public GatewayEventLogger(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void log(String gatewayId, boolean success, long latencyMs) {
        jdbc.update(
            "INSERT INTO gateway_events (gateway, success, latency_ms) VALUES (?, ?, ?)",
            gatewayId,
            success ? 1 : 0,
            latencyMs
        );
    }
}