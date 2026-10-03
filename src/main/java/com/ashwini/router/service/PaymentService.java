package com.ashwini.router.service;

import com.ashwini.router.config.RouterProperties;
import com.ashwini.router.gateway.GatewayEventLogger;
import com.ashwini.router.gateway.GatewayRegistry;
import com.ashwini.router.gateway.GatewayState;
import com.ashwini.router.gateway.MockGateway;
import com.ashwini.router.model.PaymentRequest;
import com.ashwini.router.model.PaymentResponse;
import com.ashwini.router.repo.PaymentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepo;
    private final GatewayRegistry registry;
    private final GatewayEventLogger eventLogger;
    private final MockGateway mockGateway;
    private final RouterProperties routerProps;
    private final ObjectMapper objectMapper;

    public PaymentService(PaymentRepository paymentRepo,
                          GatewayRegistry registry,
                          GatewayEventLogger eventLogger,
                          MockGateway mockGateway,
                          RouterProperties routerProps,
                          ObjectMapper objectMapper) {
        this.paymentRepo = paymentRepo;
        this.registry = registry;
        this.eventLogger = eventLogger;
        this.mockGateway = mockGateway;
        this.routerProps = routerProps;
        this.objectMapper = objectMapper;
    }

    public PaymentResponse processPayment(PaymentRequest req) {
        String key = req.getIdempotencyKey();

        // === Step 1: idempotency — try to claim the key ===
        Optional<PaymentRepository.PaymentRow> existing =
            paymentRepo.tryClaim(key, req.getAmount(), req.getCurrency());

        if (existing.isPresent()) {
            return buildDuplicateResponse(existing.get());
        }

        // === Step 2: we own the key → route the payment ===
        PaymentResponse response = routeAndProcess(req);

        // === Step 3: persist the final result ===
        try {
            String body = objectMapper.writeValueAsString(response);
            paymentRepo.finalizePayment(
                key,
                response.getStatus(),
                response.getGateway(),
                response.getAttempts(),
                body
            );
        } catch (Exception e) {
            // If persistence fails, at least the payment was attempted.
            System.err.println("[PaymentService] failed to persist result: " + e.getMessage());
        }

        return response;
    }

    private PaymentResponse routeAndProcess(PaymentRequest req) {
        List<GatewayState> ranked = registry.rankedByEma();
        int maxAttempts = Math.min(routerProps.getMaxAttempts(), ranked.size());
        double alpha = routerProps.getEmaAlpha();

        List<String> attempted = new ArrayList<>();
        String lastError = null;

        for (int i = 0; i < maxAttempts; i++) {
            GatewayState gw = ranked.get(i);
            attempted.add(gw.getId());

            MockGateway.GatewayCallResult result =
                mockGateway.process(gw, req.getAmount());

            // update EMA + counters
            gw.recordOutcome(result.isSuccess(), result.getLatencyMs(), alpha);

            // log to DB
            eventLogger.log(gw.getId(), result.isSuccess(), result.getLatencyMs());

            if (result.isSuccess()) {
                PaymentResponse resp = new PaymentResponse();
                resp.setIdempotencyKey(req.getIdempotencyKey());
                resp.setStatus("SUCCESS");
                resp.setGateway(gw.getId());
                resp.setAttempts(attempted.size());
                resp.setAttemptedGateways(attempted);
                resp.setTransactionId(result.getTransactionId());
                resp.setMessage("Payment processed successfully");
                resp.setCreatedAt(Instant.now());
                resp.setDuplicate(false);
                return resp;
            } else {
                lastError = result.getError();
            }
        }

        // All attempts failed
        PaymentResponse resp = new PaymentResponse();
        resp.setIdempotencyKey(req.getIdempotencyKey());
        resp.setStatus("FAILED");
        resp.setGateway(attempted.isEmpty() ? null : attempted.get(attempted.size() - 1));
        resp.setAttempts(attempted.size());
        resp.setAttemptedGateways(attempted);
        resp.setMessage("All gateways failed. Last error: " + lastError);
        resp.setCreatedAt(Instant.now());
        resp.setDuplicate(false);
        return resp;
    }

    private PaymentResponse buildDuplicateResponse(PaymentRepository.PaymentRow row) {
        PaymentResponse resp = new PaymentResponse();
        resp.setIdempotencyKey(row.getIdempotencyKey());
        resp.setStatus(row.getStatus());
        resp.setGateway(row.getGateway());
        resp.setAttempts(row.getAttempts());
        resp.setCreatedAt(row.getCreatedAt());
        resp.setDuplicate(true);

        if (row.getResponseBody() != null) {
            try {
                PaymentResponse original = objectMapper.readValue(
                    row.getResponseBody(), PaymentResponse.class);
                resp.setAttemptedGateways(original.getAttemptedGateways());
                resp.setTransactionId(original.getTransactionId());
                resp.setMessage("Duplicate request — original response returned");
            } catch (Exception e) {
                resp.setMessage("Duplicate request");
            }
        } else {
            resp.setMessage("Duplicate request — original still in progress");
        }
        return resp;
    }
}