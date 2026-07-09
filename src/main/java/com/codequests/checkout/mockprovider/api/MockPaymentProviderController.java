package com.codequests.checkout.mockprovider.api;

import com.codequests.checkout.mockprovider.application.MockPaymentProviderService;
import com.codequests.checkout.mockprovider.dto.TriggerPaymentResultRequest;
import com.codequests.checkout.payment.dto.PaymentWebhookResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/mock-provider/payments")
@RequiredArgsConstructor
@Tag(name = "Mock Payment Provider", description = "Mock payment provider for testing")
public class MockPaymentProviderController {

    private final MockPaymentProviderService mockPaymentProviderService;

    @PostMapping("/{paymentId}/result")
    @Operation(summary = "Trigger a payment result (CONFIRMED or FAILED)")
    public ResponseEntity<PaymentWebhookResponse> triggerResult(
            @PathVariable Long paymentId,
            @Valid @RequestBody TriggerPaymentResultRequest request) {
        return ResponseEntity.ok(mockPaymentProviderService.triggerResult(paymentId, request));
    }
}

