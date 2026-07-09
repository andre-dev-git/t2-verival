package com.codequests.checkout.payment.dto;

import com.codequests.checkout.payment.domain.PaymentResult;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentWebhookRequest {
    @NotNull(message = "Payment ID is required")
    private Long paymentId;

    @NotNull(message = "Payment result is required")
    private PaymentResult result;
}

