package com.codequests.checkout;

import com.codequests.checkout.cart.dto.AddCartItemRequest;
import com.codequests.checkout.cart.dto.CartResponse;
import com.codequests.checkout.order.dto.OrderResponse;
import com.codequests.checkout.payment.domain.PaymentResult;
import com.codequests.checkout.payment.dto.PaymentResponse;
import com.codequests.checkout.payment.dto.PaymentWebhookRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@WebAppConfiguration
@ActiveProfiles("test")
@DisplayName("Payment Retry History System Test")
class PaymentRetryHistorySystemTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("Após retry confirmado, o pagamento falho fica preservado e um webhook tardio nele é ignorado")
    @DirtiesContext
    void retryPreservesFailedPaymentHistoryAndIgnoresLateWebhook() throws Exception {
        Long cartId = createCartWithItem(1L, 1);
        Long orderId = checkoutCart(cartId);

        Long payment1Id = startPayment(orderId);
        sendWebhook(payment1Id, PaymentResult.FAILED)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value("FAILED"))
                .andExpect(jsonPath("$.orderStatus").value("PAYMENT_FAILED"));

        Long payment2Id = startPayment(orderId);
        assertThat(payment2Id).isNotEqualTo(payment1Id);
        sendWebhook(payment2Id, PaymentResult.CONFIRMED)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value("CONFIRMED"))
                .andExpect(jsonPath("$.orderStatus").value("PAID"));

        mockMvc.perform(get("/payments/" + payment1Id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(payment1Id))
                .andExpect(jsonPath("$.status").value("FAILED"));

        mockMvc.perform(get("/payments/" + payment2Id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        sendWebhook(payment1Id, PaymentResult.FAILED)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value("FAILED"))
                .andExpect(jsonPath("$.message").value("Duplicate webhook ignored safely"));

        mockMvc.perform(get("/orders/" + orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    private org.springframework.test.web.servlet.ResultActions sendWebhook(Long paymentId, PaymentResult result) throws Exception {
        PaymentWebhookRequest request = new PaymentWebhookRequest(paymentId, result);
        return mockMvc.perform(post("/payments/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private Long createCartWithItem(Long productId, int quantity) throws Exception {
        MvcResult cartResult = mockMvc.perform(post("/carts"))
                .andExpect(status().isCreated())
                .andReturn();

        CartResponse cart = objectMapper.readValue(
                cartResult.getResponse().getContentAsString(),
                CartResponse.class
        );

        AddCartItemRequest addItemRequest = new AddCartItemRequest(productId, quantity);
        mockMvc.perform(post("/carts/" + cart.getId() + "/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addItemRequest)))
                .andExpect(status().isOk());

        return cart.getId();
    }

    private Long checkoutCart(Long cartId) throws Exception {
        MvcResult orderResult = mockMvc.perform(post("/carts/" + cartId + "/checkout"))
                .andExpect(status().isOk())
                .andReturn();

        OrderResponse order = objectMapper.readValue(
                orderResult.getResponse().getContentAsString(),
                OrderResponse.class
        );

        return order.getId();
    }

    private Long startPayment(Long orderId) throws Exception {
        MvcResult paymentResult = mockMvc.perform(post("/orders/" + orderId + "/payment/start"))
                .andExpect(status().isOk())
                .andReturn();

        PaymentResponse payment = objectMapper.readValue(
                paymentResult.getResponse().getContentAsString(),
                PaymentResponse.class
        );

        return payment.getId();
    }
}
