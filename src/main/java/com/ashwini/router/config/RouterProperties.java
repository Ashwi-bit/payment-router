package com.ashwini.router.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "router")
public class RouterProperties {

    private double emaAlpha = 0.3;
    private int maxAttempts = 3;
    private int priorSuccess = 5;
    private int priorFailure = 5;

    public double getEmaAlpha() { return emaAlpha; }
    public void setEmaAlpha(double emaAlpha) { this.emaAlpha = emaAlpha; }

    public int getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }

    public int getPriorSuccess() { return priorSuccess; }
    public void setPriorSuccess(int priorSuccess) { this.priorSuccess = priorSuccess; }

    public int getPriorFailure() { return priorFailure; }
    public void setPriorFailure(int priorFailure) { this.priorFailure = priorFailure; }
}