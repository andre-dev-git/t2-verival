package com.codequests.checkout.mockprovider.dto;

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
public class TriggerPaymentResultRequest {
    @NotNull(message = "Payment result is required")
    private PaymentResult result;
}

