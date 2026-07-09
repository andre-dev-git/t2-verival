package com.codequests.checkout;

import com.codequests.checkout.cart.dto.AddCartItemRequest;
import com.codequests.checkout.cart.dto.CartResponse;
import com.codequests.checkout.order.dto.OrderResponse;
import com.codequests.checkout.payment.domain.PaymentResult;
import com.codequests.checkout.payment.dto.PaymentResponse;
import com.codequests.checkout.payment.dto.PaymentWebhookRequest;
import com.codequests.checkout.product.dto.ProductResponse;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@WebAppConfiguration
@ActiveProfiles("test")
@DisplayName("Checkout and Payment Flow Integration Tests")
class CheckoutPaymentFlowIntegrationTest {

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
    @DisplayName("Happy path: Create cart, add items, checkout, start payment, confirm payment")
    @DirtiesContext
    void happyPath() throws Exception {
        MvcResult productResult = mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andReturn();

        ProductResponse[] products = objectMapper.readValue(
                productResult.getResponse().getContentAsString(),
                ProductResponse[].class
        );
        assertThat(products).hasSizeGreaterThan(0);
        Long productId = products[0].getId();

        MvcResult cartResult = mockMvc.perform(post("/carts"))
                .andExpect(status().isCreated())
                .andReturn();

        CartResponse cart = objectMapper.readValue(
                cartResult.getResponse().getContentAsString(),
                CartResponse.class
        );
        Long cartId = cart.getId();

        AddCartItemRequest addItemRequest = new AddCartItemRequest(productId, 2);
        mockMvc.perform(post("/carts/" + cartId + "/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addItemRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].quantity").value(2));

        MvcResult orderResult = mockMvc.perform(post("/carts/" + cartId + "/checkout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andReturn();

        OrderResponse order = objectMapper.readValue(
                orderResult.getResponse().getContentAsString(),
                OrderResponse.class
        );
        Long orderId = order.getId();

        MvcResult paymentResult = mockMvc.perform(post("/orders/" + orderId + "/payment/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();

        PaymentResponse payment = objectMapper.readValue(
                paymentResult.getResponse().getContentAsString(),
                PaymentResponse.class
        );
        Long paymentId = payment.getId();

        PaymentWebhookRequest webhookRequest = new PaymentWebhookRequest(paymentId, PaymentResult.CONFIRMED);
        mockMvc.perform(post("/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(webhookRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value("CONFIRMED"))
                .andExpect(jsonPath("$.orderStatus").value("PAID"))
                .andExpect(jsonPath("$.message").value("Payment confirmed successfully"))
                .andReturn();

        mockMvc.perform(get("/orders/" + orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    @DisplayName("Payment failure then retry")
    @DirtiesContext
    void paymentFailureThenRetry() throws Exception {
        Long cartId = createCartWithItem(1L, 1);
        Long orderId = checkoutCart(cartId);

        MvcResult paymentResult1 = mockMvc.perform(post("/orders/" + orderId + "/payment/start"))
                .andExpect(status().isOk())
                .andReturn();

        PaymentResponse payment1 = objectMapper.readValue(
                paymentResult1.getResponse().getContentAsString(),
                PaymentResponse.class
        );
        Long payment1Id = payment1.getId();

        PaymentWebhookRequest failRequest = new PaymentWebhookRequest(payment1Id, PaymentResult.FAILED);
        mockMvc.perform(post("/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(failRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value("FAILED"))
                .andExpect(jsonPath("$.orderStatus").value("PAYMENT_FAILED"));

        mockMvc.perform(get("/orders/" + orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAYMENT_FAILED"));

        MvcResult paymentResult2 = mockMvc.perform(post("/orders/" + orderId + "/payment/start"))
                .andExpect(status().isOk())
                .andReturn();

        PaymentResponse payment2 = objectMapper.readValue(
                paymentResult2.getResponse().getContentAsString(),
                PaymentResponse.class
        );
        Long payment2Id = payment2.getId();

        assertThat(payment2Id).isNotEqualTo(payment1Id);

        PaymentWebhookRequest confirmRequest = new PaymentWebhookRequest(payment2Id, PaymentResult.CONFIRMED);
        mockMvc.perform(post("/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(confirmRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderStatus").value("PAID"));
    }

    @Test
    @DisplayName("Duplicate confirmed webhook is handled safely")
    @DirtiesContext
    void duplicateConfirmedWebhook() throws Exception {
        Long cartId = createCartWithItem(1L, 1);
        Long orderId = checkoutCart(cartId);
        Long paymentId = startPayment(orderId);

        PaymentWebhookRequest webhookRequest = new PaymentWebhookRequest(paymentId, PaymentResult.CONFIRMED);
        mockMvc.perform(post("/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(webhookRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Payment confirmed successfully"));

        mockMvc.perform(post("/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(webhookRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value("CONFIRMED"))
                .andExpect(jsonPath("$.orderStatus").value("PAID"))
                .andExpect(jsonPath("$.message").value("Duplicate webhook ignored safely"));

        mockMvc.perform(get("/orders/" + orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    @DisplayName("Failed webhook after confirmed payment is ignored")
    @DirtiesContext
    void failedWebhookAfterConfirmedPayment() throws Exception {
        Long cartId = createCartWithItem(1L, 1);
        Long orderId = checkoutCart(cartId);
        Long paymentId = startPayment(orderId);

        PaymentWebhookRequest confirmRequest = new PaymentWebhookRequest(paymentId, PaymentResult.CONFIRMED);
        mockMvc.perform(post("/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(confirmRequest)))
                .andExpect(status().isOk());

        PaymentWebhookRequest failRequest = new PaymentWebhookRequest(paymentId, PaymentResult.FAILED);
        mockMvc.perform(post("/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(failRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value("CONFIRMED"))
                .andExpect(jsonPath("$.orderStatus").value("PAID"))
                .andExpect(jsonPath("$.message").value("Payment already finalized; webhook ignored"));

        mockMvc.perform(get("/orders/" + orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    @DisplayName("Payment start idempotency: starting payment twice returns same payment")
    @DirtiesContext
    void paymentStartIdempotency() throws Exception {
        Long cartId = createCartWithItem(1L, 1);
        Long orderId = checkoutCart(cartId);

        MvcResult result1 = mockMvc.perform(post("/orders/" + orderId + "/payment/start"))
                .andExpect(status().isOk())
                .andReturn();

        PaymentResponse payment1 = objectMapper.readValue(
                result1.getResponse().getContentAsString(),
                PaymentResponse.class
        );

        MvcResult result2 = mockMvc.perform(post("/orders/" + orderId + "/payment/start"))
                .andExpect(status().isOk())
                .andReturn();

        PaymentResponse payment2 = objectMapper.readValue(
                result2.getResponse().getContentAsString(),
                PaymentResponse.class
        );

        assertThat(payment1.getId()).isEqualTo(payment2.getId());
    }

    @Test
    @DisplayName("Checkout idempotency: checking out same cart twice returns same order")
    @DirtiesContext
    void checkoutIdempotency() throws Exception {
        Long cartId = createCartWithItem(1L, 2);

        MvcResult result1 = mockMvc.perform(post("/carts/" + cartId + "/checkout"))
                .andExpect(status().isOk())
                .andReturn();

        OrderResponse order1 = objectMapper.readValue(
                result1.getResponse().getContentAsString(),
                OrderResponse.class
        );

        MvcResult result2 = mockMvc.perform(post("/carts/" + cartId + "/checkout"))
                .andExpect(status().isOk())
                .andReturn();

        OrderResponse order2 = objectMapper.readValue(
                result2.getResponse().getContentAsString(),
                OrderResponse.class
        );

        assertThat(order1.getId()).isEqualTo(order2.getId());
    }

    @Test
    @DisplayName("Empty cart checkout returns error")
    @DirtiesContext
    void emptyCartCheckout() throws Exception {
        MvcResult cartResult = mockMvc.perform(post("/carts"))
                .andExpect(status().isCreated())
                .andReturn();

        CartResponse cart = objectMapper.readValue(
                cartResult.getResponse().getContentAsString(),
                CartResponse.class
        );

        mockMvc.perform(post("/carts/" + cart.getId() + "/checkout"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("EMPTY_CART"));
    }

    @Test
    @DisplayName("Insufficient stock returns error")
    @DirtiesContext
    void insufficientStock() throws Exception {
        MvcResult cartResult = mockMvc.perform(post("/carts"))
                .andExpect(status().isCreated())
                .andReturn();

        CartResponse cart = objectMapper.readValue(
                cartResult.getResponse().getContentAsString(),
                CartResponse.class
        );

        AddCartItemRequest request = new AddCartItemRequest(1L, 1000);
        mockMvc.perform(post("/carts/" + cart.getId() + "/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INSUFFICIENT_STOCK"));
    }

    @Test
    @DisplayName("Stock race boundary: first checkout succeeds and second checkout fails with insufficient stock")
    @DirtiesContext
    void concurrentCheckoutProductAvailabilityRaceCondition() throws Exception {
        Long productId = 1L;

        Long cartId1 = createCartWithItem(productId, 15);
        Long cartId2 = createCartWithItem(productId, 1);

        MvcResult firstCheckout = mockMvc.perform(post("/carts/" + cartId1 + "/checkout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andReturn();

        MvcResult secondCheckout = mockMvc.perform(post("/carts/" + cartId2 + "/checkout"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INSUFFICIENT_STOCK"))
                .andReturn();

        assertThat(firstCheckout.getResponse().getContentAsString()).contains("\"status\":\"CREATED\"");
        assertThat(secondCheckout.getResponse().getContentAsString()).contains("Insufficient stock for product");
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

