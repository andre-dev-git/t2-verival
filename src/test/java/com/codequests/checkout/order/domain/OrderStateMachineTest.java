package com.codequests.checkout.order.domain;

import com.codequests.checkout.shared.exception.InvalidStateException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Order State Machine Tests")
class OrderStateMachineTest {

    @Test
    @DisplayName("CREATED can transition to PENDING_PAYMENT")
    void createdCanTransitionToPendingPayment() {
        Order order = new Order();
        order.setStatus(OrderStatus.CREATED);

        order.startPayment();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
    }

    @Test
    @DisplayName("PAYMENT_FAILED can transition to PENDING_PAYMENT")
    void paymentFailedCanRetryPayment() {
        Order order = new Order();
        order.setStatus(OrderStatus.PAYMENT_FAILED);

        order.startPayment();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
    }

    @Test
    @DisplayName("PENDING_PAYMENT can transition to PAID")
    void pendingPaymentCanTransitionToPaid() {
        Order order = new Order();
        order.setStatus(OrderStatus.PENDING_PAYMENT);

        order.markPaid();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    @DisplayName("PENDING_PAYMENT can transition to PAYMENT_FAILED")
    void pendingPaymentCanTransitionToFailed() {
        Order order = new Order();
        order.setStatus(OrderStatus.PENDING_PAYMENT);

        order.markPaymentFailed();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYMENT_FAILED);
    }

    @Test
    @DisplayName("CREATED cannot transition directly to PAID")
    void createdCannotTransitionDirectlyToPaid() {
        Order order = new Order();
        order.setStatus(OrderStatus.CREATED);

        assertThatThrownBy(order::markPaid)
                .isInstanceOf(InvalidStateException.class)
                .hasMessageContaining("Cannot mark order as paid from status CREATED");
    }

    @Test
    @DisplayName("PAID cannot start payment again")
    void paidCannotStartPaymentAgain() {
        Order order = new Order();
        order.setStatus(OrderStatus.PAID);

        assertThatThrownBy(order::startPayment)
                .isInstanceOf(InvalidStateException.class)
                .hasMessageContaining("Cannot start payment for an already paid order");
    }

    @Test
    @DisplayName("PAID cannot be marked as failed")
    void paidCannotBeMarkedAsFailed() {
        Order order = new Order();
        order.setStatus(OrderStatus.PAID);

        assertThatThrownBy(order::markPaymentFailed)
                .isInstanceOf(InvalidStateException.class)
                .hasMessageContaining("Cannot mark a paid order as failed");
    }

    @Test
    @DisplayName("PENDING_PAYMENT remains PENDING_PAYMENT when startPayment is called again")
    void pendingPaymentRemainsWhenStartPaymentCalledAgain() {
        Order order = new Order();
        order.setStatus(OrderStatus.PENDING_PAYMENT);

        order.startPayment();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
    }
}

