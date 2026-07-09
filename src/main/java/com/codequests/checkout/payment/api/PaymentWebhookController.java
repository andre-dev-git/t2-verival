package com.codequests.checkout.payment.api;

import com.codequests.checkout.payment.application.PaymentService;
import com.codequests.checkout.payment.dto.PaymentWebhookRequest;
import com.codequests.checkout.payment.dto.PaymentWebhookResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Tag(name = "Payment Webhooks", description = "Payment webhook processing")
public class PaymentWebhookController {

    private final PaymentService paymentService;

    @PostMapping("/webhook")
    @Operation(summary = "Process payment webhook from provider")
    public ResponseEntity<PaymentWebhookResponse> handleWebhook(@Valid @RequestBody PaymentWebhookRequest request) {
        return ResponseEntity.ok(paymentService.handleWebhook(request));
    }
}

