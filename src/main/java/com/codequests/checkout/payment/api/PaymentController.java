package com.codequests.checkout.payment.api;

import com.codequests.checkout.payment.application.PaymentService;
import com.codequests.checkout.payment.dto.PaymentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Payment operations")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/orders/{orderId}/payment/start")
    @Operation(summary = "Start payment for an order")
    public ResponseEntity<PaymentResponse> startPayment(@PathVariable Long orderId) {
        return ResponseEntity.ok(paymentService.startPayment(orderId));
    }

    @GetMapping("/payments/{paymentId}")
    @Operation(summary = "Get payment by ID")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable Long paymentId) {
        return ResponseEntity.ok(paymentService.getPayment(paymentId));
    }
}

