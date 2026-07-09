package com.codequests.checkout.payment.repository;

import com.codequests.checkout.cart.domain.Cart;
import com.codequests.checkout.order.domain.Order;
import com.codequests.checkout.order.domain.OrderStatus;
import com.codequests.checkout.payment.domain.Payment;
import com.codequests.checkout.payment.domain.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("PaymentRepository - testes de integração (@DataJpaTest)")
class PaymentRepositoryIntegrationTest {

    private static final BigDecimal AMOUNT = new BigDecimal("240.00");

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    @DisplayName("D1 - retry: com FAILED antigo e PENDING novo, retorna o PENDING preservando o histórico")
    void d1_retryReturnsPendingAndKeepsFailedHistory() {
        Order order = persistOrder(OrderStatus.PENDING_PAYMENT);
        Payment failed = persistPayment(order, PaymentStatus.FAILED);
        Payment pending = persistPayment(order, PaymentStatus.PENDING);
        flushAndClear();

        Optional<Payment> foundPending = paymentRepository
                .findFirstByOrderIdAndStatus(order.getId(), PaymentStatus.PENDING);

        assertThat(foundPending).isPresent();
        assertThat(foundPending.get().getId()).isEqualTo(pending.getId());
        assertThat(foundPending.get().getStatus()).isEqualTo(PaymentStatus.PENDING);

        Optional<Payment> foundFailed = paymentRepository
                .findFirstByOrderIdAndStatus(order.getId(), PaymentStatus.FAILED);
        assertThat(foundFailed).isPresent();
        assertThat(foundFailed.get().getId()).isEqualTo(failed.getId());

        assertThat(paymentRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("D2 - só existe pagamento FAILED: busca por PENDING retorna vazio (força criação no retry)")
    void d2_onlyFailedReturnsEmptyForPending() {
        Order order = persistOrder(OrderStatus.PAYMENT_FAILED);
        persistPayment(order, PaymentStatus.FAILED);
        flushAndClear();

        Optional<Payment> foundPending = paymentRepository
                .findFirstByOrderIdAndStatus(order.getId(), PaymentStatus.PENDING);

        assertThat(foundPending).isEmpty();
    }

    @Test
    @DisplayName("D3 - @EntityGraph carrega a associação order junto do pagamento")
    void d3_entityGraphLoadsOrderAssociation() {
        Order order = persistOrder(OrderStatus.PENDING_PAYMENT);
        persistPayment(order, PaymentStatus.PENDING);
        flushAndClear();

        Optional<Payment> found = paymentRepository
                .findFirstByOrderIdAndStatus(order.getId(), PaymentStatus.PENDING);

        assertThat(found).isPresent();
        assertThat(found.get().getOrder()).isNotNull();
        assertThat(found.get().getOrder().getId()).isEqualTo(order.getId());
        assertThat(found.get().getOrder().getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
    }

    private Order persistOrder(OrderStatus status) {
        Cart cart = new Cart();
        entityManager.persist(cart);

        Order order = new Order();
        order.setCart(cart);
        order.setStatus(status);
        order.setTotalAmount(AMOUNT);
        return entityManager.persist(order);
    }

    private Payment persistPayment(Order order, PaymentStatus status) {
        Payment payment = new Payment(order, AMOUNT);
        payment.setStatus(status);
        return entityManager.persist(payment);
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
