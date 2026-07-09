package com.codequests.checkout.payment.domain;

import com.codequests.checkout.shared.exception.InvalidStateException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Payment Domain Tests")
class PaymentDomainTest {

    @Test
    @DisplayName("PENDING payment can be confirmed")
    void pendingPaymentCanBeConfirmed() {
        Payment payment = new Payment();
        payment.setStatus(PaymentStatus.PENDING);

        payment.confirm();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CONFIRMED);
    }

    @Test
    @DisplayName("PENDING payment can fail")
    void pendingPaymentCanFail() {
        Payment payment = new Payment();
        payment.setStatus(PaymentStatus.PENDING);

        payment.fail();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    @DisplayName("FAILED payment cannot become CONFIRMED")
    void failedPaymentCannotBecomeConfirmed() {
        Payment payment = new Payment();
        payment.setStatus(PaymentStatus.FAILED);

        assertThatThrownBy(payment::confirm)
                .isInstanceOf(InvalidStateException.class)
                .hasMessageContaining("Cannot confirm a failed payment");
    }

    @Test
    @DisplayName("CONFIRMED payment cannot become FAILED")
    void confirmedPaymentCannotBecomeFailed() {
        Payment payment = new Payment();
        payment.setStatus(PaymentStatus.CONFIRMED);

        assertThatThrownBy(payment::fail)
                .isInstanceOf(InvalidStateException.class)
                .hasMessageContaining("Cannot fail a confirmed payment");
    }

    @Test
    @DisplayName("CONFIRMED payment can be confirmed again (idempotency)")
    void confirmedPaymentCanBeConfirmedAgain() {
        Payment payment = new Payment();
        payment.setStatus(PaymentStatus.CONFIRMED);

        payment.confirm();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CONFIRMED);
    }

    @Test
    @DisplayName("FAILED payment can fail again (idempotency)")
    void failedPaymentCanFailAgain() {
        Payment payment = new Payment();
        payment.setStatus(PaymentStatus.FAILED);

        payment.fail();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }
}

