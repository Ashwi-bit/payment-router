package com.ashwini.router.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;


@ConfigurationProperties(prefix = "gateways")
public class GatewaysProperties {

    private List<GatewayConfig> list = new ArrayList<>();

    public List<GatewayConfig> getList() { return list; }
    public void setList(List<GatewayConfig> list) { this.list = list; }

    public static class GatewayConfig {
        private String id;
        private double failureRate;
        private int latencyMs;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public double getFailureRate() { return failureRate; }
        public void setFailureRate(double failureRate) { this.failureRate = failureRate; }

        public int getLatencyMs() { return latencyMs; }
        public void setLatencyMs(int latencyMs) { this.latencyMs = latencyMs; }
    }
}