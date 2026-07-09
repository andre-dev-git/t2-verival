package com.codequests.checkout.payment.application;

import com.codequests.checkout.order.application.OrderService;
import com.codequests.checkout.order.domain.Order;
import com.codequests.checkout.payment.domain.Payment;
import com.codequests.checkout.payment.domain.PaymentResult;
import com.codequests.checkout.payment.domain.PaymentStatus;
import com.codequests.checkout.payment.dto.PaymentResponse;
import com.codequests.checkout.payment.dto.PaymentWebhookRequest;
import com.codequests.checkout.payment.dto.PaymentWebhookResponse;
import com.codequests.checkout.payment.mapper.PaymentMapper;
import com.codequests.checkout.payment.repository.PaymentRepository;
import com.codequests.checkout.shared.constants.BusinessMessages;
import com.codequests.checkout.shared.constants.ErrorCodes;
import com.codequests.checkout.shared.constants.ErrorMessages;
import com.codequests.checkout.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderService orderService;

    private final PaymentMapper paymentMapper;

    @Transactional
    public PaymentResponse startPayment(Long orderId) {
        Order order = orderService.findOrder(orderId);

        Payment existingPendingPayment = paymentRepository
                .findFirstByOrderIdAndStatus(orderId, PaymentStatus.PENDING)
                .orElse(null);

        if (existingPendingPayment != null) {
            return paymentMapper.toResponse(existingPendingPayment);
        }

        order.startPayment();

        Payment payment = new Payment(order, order.getTotalAmount());
        payment = paymentRepository.save(payment);

        return paymentMapper.toResponse(payment);
    }

    @Transactional
    public PaymentWebhookResponse handleWebhook(PaymentWebhookRequest request) {
        Payment payment = findPayment(request.getPaymentId());
        Order order = payment.getOrder();

        PaymentStatus currentPaymentStatus = payment.getStatus();
        PaymentResult requestedResult = request.getResult();

        String message;

        if (currentPaymentStatus == PaymentStatus.PENDING) {
            if (requestedResult == PaymentResult.CONFIRMED) {
                payment.confirm();
                order.markPaid();
                paymentRepository.save(payment);
                orderService.saveOrder(order);
                message = BusinessMessages.PAYMENT_CONFIRMED_SUCCESSFULLY;
            } else {
                payment.fail();
                order.markPaymentFailed();
                paymentRepository.save(payment);
                orderService.saveOrder(order);
                message = BusinessMessages.PAYMENT_FAILED;
            }
        } else if (currentPaymentStatus == PaymentStatus.CONFIRMED) {
            if (requestedResult == PaymentResult.CONFIRMED) {
                message = BusinessMessages.DUPLICATE_WEBHOOK_IGNORED;
            } else {
                message = BusinessMessages.PAYMENT_ALREADY_FINALIZED;
            }
        } else {
            if (requestedResult == PaymentResult.FAILED) {
                message = BusinessMessages.DUPLICATE_WEBHOOK_IGNORED;
            } else {
                message = BusinessMessages.PAYMENT_ALREADY_FINALIZED;
            }
        }

        return new PaymentWebhookResponse(
                payment.getId(),
                payment.getStatus(),
                order.getId(),
                order.getStatus(),
                message
        );
    }

    public PaymentResponse getPayment(Long paymentId) {
        Payment payment = findPayment(paymentId);
        return paymentMapper.toResponse(payment);
    }

    public Payment findPayment(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.PAYMENT_NOT_FOUND, String.format(ErrorMessages.PAYMENT_NOT_FOUND, paymentId)));
    }
}

