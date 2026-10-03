package com.ashwini.router.controller;

import com.ashwini.router.model.PaymentRequest;
import com.ashwini.router.model.PaymentResponse;
import com.ashwini.router.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/payments")
    public ResponseEntity<PaymentResponse> pay(@Valid @RequestBody PaymentRequest req) {
        PaymentResponse resp = paymentService.processPayment(req);

        HttpStatus status = "SUCCESS".equals(resp.getStatus())
            ? HttpStatus.OK
            : HttpStatus.PAYMENT_REQUIRED; // 402

        return ResponseEntity.status(status).body(resp);
    }
}