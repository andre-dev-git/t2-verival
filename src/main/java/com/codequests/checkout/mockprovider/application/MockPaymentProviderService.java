package com.codequests.checkout.mockprovider.application;

import com.codequests.checkout.mockprovider.dto.TriggerPaymentResultRequest;
import com.codequests.checkout.payment.dto.PaymentWebhookRequest;
import com.codequests.checkout.payment.dto.PaymentWebhookResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class MockPaymentProviderService {
    private final RestClient webhookRestClient;

    public PaymentWebhookResponse triggerResult(Long paymentId, TriggerPaymentResultRequest request) {
        PaymentWebhookRequest webhookRequest = new PaymentWebhookRequest(paymentId, request.getResult());
        PaymentWebhookResponse response = webhookRestClient.post()
                .uri("/payments/webhook")
                .body(webhookRequest)
                .retrieve()
                .body(PaymentWebhookResponse.class);

        return response;
    }
}

