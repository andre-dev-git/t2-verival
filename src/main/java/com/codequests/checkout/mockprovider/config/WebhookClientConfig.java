package com.codequests.checkout.mockprovider.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class WebhookClientConfig {

    @Value("${mock-provider.webhook-base-url}")
    private String webhookBaseUrl;

    @Bean
    public RestClient webhookRestClient() {
        return RestClient.builder()
                .baseUrl(webhookBaseUrl)
                .build();
    }
}

