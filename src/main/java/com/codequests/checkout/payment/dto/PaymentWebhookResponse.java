package com.codequests.checkout.payment.dto;

import com.codequests.checkout.order.domain.OrderStatus;
import com.codequests.checkout.payment.domain.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentWebhookResponse {
    private Long paymentId;
    private PaymentStatus paymentStatus;
    private Long orderId;
    private OrderStatus orderStatus;
    private String message;
}

