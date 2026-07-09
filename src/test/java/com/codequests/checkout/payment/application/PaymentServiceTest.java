package com.codequests.checkout.payment.application;

import com.codequests.checkout.order.application.OrderService;
import com.codequests.checkout.order.domain.Order;
import com.codequests.checkout.order.domain.OrderStatus;
import com.codequests.checkout.payment.domain.Payment;
import com.codequests.checkout.payment.domain.PaymentResult;
import com.codequests.checkout.payment.domain.PaymentStatus;
import com.codequests.checkout.payment.dto.PaymentResponse;
import com.codequests.checkout.payment.dto.PaymentWebhookRequest;
import com.codequests.checkout.payment.dto.PaymentWebhookResponse;
import com.codequests.checkout.payment.mapper.PaymentMapper;
import com.codequests.checkout.payment.repository.PaymentRepository;
import com.codequests.checkout.shared.constants.BusinessMessages;
import com.codequests.checkout.shared.exception.InvalidStateException;
import com.codequests.checkout.shared.exception.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes unitários da camada de aplicação (service) isolando PaymentService com Mockito.
 * Cobre a jornada de retry de pagamento: início de pagamento e processamento do webhook.
 *
 * Estratégia: objetos de domínio reais (Order/Payment) para validar transições de estado
 * reais, com repositórios e OrderService mockados para isolar a lógica de orquestração.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService - testes unitários (Mockito)")
class PaymentServiceTest {

    private static final Long ORDER_ID = 1L;
    private static final Long PAYMENT_ID = 10L;
    private static final BigDecimal ORDER_TOTAL = new BigDecimal("240.00");

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderService orderService;

    @Mock
    private PaymentMapper paymentMapper;

    @InjectMocks
    private PaymentService paymentService;

    @Nested
    @DisplayName("startPayment - partição de equivalência por estado do pedido")
    class StartPayment {

        @Test
        @DisplayName("A1 - order CREATED sem pagamento pendente cria novo pagamento PENDING")
        void a1_createdWithoutPendingCreatesNewPayment() {
            Order order = order(OrderStatus.CREATED);
            when(orderService.findOrder(ORDER_ID)).thenReturn(order);
            when(paymentRepository.findFirstByOrderIdAndStatus(ORDER_ID, PaymentStatus.PENDING))
                    .thenReturn(Optional.empty());
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
            when(paymentMapper.toResponse(any(Payment.class))).thenReturn(new PaymentResponse());

            paymentService.startPayment(ORDER_ID);

            // A order deve transitar de CREATED para PENDING_PAYMENT
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);

            ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
            verify(paymentRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(PaymentStatus.PENDING);
            assertThat(captor.getValue().getAmount()).isEqualByComparingTo(ORDER_TOTAL);
        }

        @Test
        @DisplayName("A2 - order PENDING_PAYMENT com pagamento pendente retorna o existente (idempotência)")
        void a2_pendingWithExistingPaymentReturnsExisting() {
            Order order = order(OrderStatus.PENDING_PAYMENT);
            Payment existing = payment(PaymentStatus.PENDING, order);
            when(orderService.findOrder(ORDER_ID)).thenReturn(order);
            when(paymentRepository.findFirstByOrderIdAndStatus(ORDER_ID, PaymentStatus.PENDING))
                    .thenReturn(Optional.of(existing));
            PaymentResponse existingResponse = new PaymentResponse();
            existingResponse.setId(PAYMENT_ID);
            when(paymentMapper.toResponse(existing)).thenReturn(existingResponse);

            PaymentResponse result = paymentService.startPayment(ORDER_ID);

            // Não deve criar/persistir novo pagamento nem alterar o estado do pedido
            verify(paymentRepository, never()).save(any());
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
            assertThat(result.getId()).isEqualTo(PAYMENT_ID);
        }

        @Test
        @DisplayName("A3 - order PAYMENT_FAILED cria novo pagamento (retry)")
        void a3_paymentFailedCreatesNewPaymentOnRetry() {
            Order order = order(OrderStatus.PAYMENT_FAILED);
            when(orderService.findOrder(ORDER_ID)).thenReturn(order);
            // O pagamento anterior está FAILED, portanto não há pagamento PENDING ativo
            when(paymentRepository.findFirstByOrderIdAndStatus(ORDER_ID, PaymentStatus.PENDING))
                    .thenReturn(Optional.empty());
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
            when(paymentMapper.toResponse(any(Payment.class))).thenReturn(new PaymentResponse());

            paymentService.startPayment(ORDER_ID);

            // O retry reinicia o pedido para PENDING_PAYMENT e cria um novo pagamento PENDING
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);

            ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
            verify(paymentRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(PaymentStatus.PENDING);
            assertThat(captor.getValue().getAmount()).isEqualByComparingTo(ORDER_TOTAL);
        }

        @Test
        @DisplayName("A4 - order PAID rejeita novo pagamento")
        void a4_paidOrderRejectsPayment() {
            Order order = order(OrderStatus.PAID);
            when(orderService.findOrder(ORDER_ID)).thenReturn(order);
            when(paymentRepository.findFirstByOrderIdAndStatus(ORDER_ID, PaymentStatus.PENDING))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.startPayment(ORDER_ID))
                    .isInstanceOf(InvalidStateException.class);

            verify(paymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("A5 - order inexistente lança NotFoundException")
        void a5_missingOrderThrowsNotFound() {
            when(orderService.findOrder(999L))
                    .thenThrow(new NotFoundException("ORDER_NOT_FOUND", "Order not found"));

            assertThatThrownBy(() -> paymentService.startPayment(999L))
                    .isInstanceOf(NotFoundException.class);

            verify(paymentRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("handleWebhook - tabela de decisão (status do pagamento x resultado recebido)")
    class HandleWebhook {

        @Test
        @DisplayName("B1 - PENDING + CONFIRMED confirma pagamento e paga o pedido")
        void b1_pendingConfirmed() {
            Order order = order(OrderStatus.PENDING_PAYMENT);
            Payment payment = payment(PaymentStatus.PENDING, order);
            when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));

            PaymentWebhookResponse response = paymentService.handleWebhook(
                    new PaymentWebhookRequest(PAYMENT_ID, PaymentResult.CONFIRMED));

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CONFIRMED);
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
            assertThat(response.getMessage()).isEqualTo(BusinessMessages.PAYMENT_CONFIRMED_SUCCESSFULLY);
            verify(paymentRepository).save(payment);
            verify(orderService).saveOrder(order);
        }

        @Test
        @DisplayName("B2 - PENDING + FAILED falha o pagamento e marca o pedido como PAYMENT_FAILED")
        void b2_pendingFailed() {
            Order order = order(OrderStatus.PENDING_PAYMENT);
            Payment payment = payment(PaymentStatus.PENDING, order);
            when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));

            PaymentWebhookResponse response = paymentService.handleWebhook(
                    new PaymentWebhookRequest(PAYMENT_ID, PaymentResult.FAILED));

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYMENT_FAILED);
            assertThat(response.getMessage()).isEqualTo(BusinessMessages.PAYMENT_FAILED);
            verify(paymentRepository).save(payment);
            verify(orderService).saveOrder(order);
        }

        @Test
        @DisplayName("B3 - CONFIRMED + CONFIRMED é webhook duplicado ignorado com segurança")
        void b3_confirmedConfirmedDuplicate() {
            Order order = order(OrderStatus.PAID);
            Payment payment = payment(PaymentStatus.CONFIRMED, order);
            when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));

            PaymentWebhookResponse response = paymentService.handleWebhook(
                    new PaymentWebhookRequest(PAYMENT_ID, PaymentResult.CONFIRMED));

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CONFIRMED);
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
            assertThat(response.getMessage()).isEqualTo(BusinessMessages.DUPLICATE_WEBHOOK_IGNORED);
            verifyNoPersistence();
        }

        @Test
        @DisplayName("B4 - CONFIRMED + FAILED é ignorado por pagamento já finalizado")
        void b4_confirmedFailedIgnored() {
            Order order = order(OrderStatus.PAID);
            Payment payment = payment(PaymentStatus.CONFIRMED, order);
            when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));

            PaymentWebhookResponse response = paymentService.handleWebhook(
                    new PaymentWebhookRequest(PAYMENT_ID, PaymentResult.FAILED));

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CONFIRMED);
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
            assertThat(response.getMessage()).isEqualTo(BusinessMessages.PAYMENT_ALREADY_FINALIZED);
            verifyNoPersistence();
        }

        @Test
        @DisplayName("B5 - FAILED + FAILED é webhook duplicado ignorado com segurança")
        void b5_failedFailedDuplicate() {
            Order order = order(OrderStatus.PAYMENT_FAILED);
            Payment payment = payment(PaymentStatus.FAILED, order);
            when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));

            PaymentWebhookResponse response = paymentService.handleWebhook(
                    new PaymentWebhookRequest(PAYMENT_ID, PaymentResult.FAILED));

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYMENT_FAILED);
            assertThat(response.getMessage()).isEqualTo(BusinessMessages.DUPLICATE_WEBHOOK_IGNORED);
            verifyNoPersistence();
        }

        @Test
        @DisplayName("B6 - FAILED + CONFIRMED é ignorado por pagamento já finalizado (evento tardio)")
        void b6_failedConfirmedIgnored() {
            Order order = order(OrderStatus.PAYMENT_FAILED);
            Payment payment = payment(PaymentStatus.FAILED, order);
            when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));

            PaymentWebhookResponse response = paymentService.handleWebhook(
                    new PaymentWebhookRequest(PAYMENT_ID, PaymentResult.CONFIRMED));

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYMENT_FAILED);
            assertThat(response.getMessage()).isEqualTo(BusinessMessages.PAYMENT_ALREADY_FINALIZED);
            verifyNoPersistence();
        }

        @Test
        @DisplayName("B7 - pagamento inexistente lança NotFoundException")
        void b7_missingPaymentThrowsNotFound() {
            when(paymentRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.handleWebhook(
                    new PaymentWebhookRequest(999L, PaymentResult.CONFIRMED)))
                    .isInstanceOf(NotFoundException.class);

            verifyNoPersistence();
        }

        // Garante que nenhum estado foi persistido nos casos idempotentes/finais
        private void verifyNoPersistence() {
            verify(paymentRepository, never()).save(any());
            verify(orderService, never()).saveOrder(any());
        }
    }

    // Cria um pedido real no estado informado, com id e total definidos
    private Order order(OrderStatus status) {
        Order order = new Order();
        order.setId(ORDER_ID);
        order.setStatus(status);
        order.setTotalAmount(ORDER_TOTAL);
        return order;
    }

    // Cria um pagamento real no estado informado, associado ao pedido
    private Payment payment(PaymentStatus status, Order order) {
        Payment payment = new Payment();
        payment.setId(PAYMENT_ID);
        payment.setStatus(status);
        payment.setAmount(ORDER_TOTAL);
        payment.setOrder(order);
        return payment;
    }
}
