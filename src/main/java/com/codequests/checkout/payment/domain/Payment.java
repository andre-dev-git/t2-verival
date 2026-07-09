package com.codequests.checkout.payment.domain;

import com.codequests.checkout.order.domain.Order;
import com.codequests.checkout.shared.constants.ErrorCodes;
import com.codequests.checkout.shared.constants.ErrorMessages;
import com.codequests.checkout.shared.domain.BaseEntity;
import com.codequests.checkout.shared.exception.InvalidStateException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Version
    private Long version;

    public Payment(Order order, BigDecimal amount) {
        this.order = order;
        this.amount = amount;
        this.status = PaymentStatus.PENDING;
    }

    public void confirm() {
        if (this.status == PaymentStatus.CONFIRMED) {
            return;
        } else if (this.status == PaymentStatus.PENDING) {
            this.status = PaymentStatus.CONFIRMED;
        } else if (this.status == PaymentStatus.FAILED) {
            throw new InvalidStateException(ErrorCodes.PAYMENT_ALREADY_FINALIZED, ErrorMessages.PAYMENT_ALREADY_FINALIZED_CONFIRM);
        }
    }

    public void fail() {
        if (this.status == PaymentStatus.FAILED) {
            return;
        } else if (this.status == PaymentStatus.PENDING) {
            this.status = PaymentStatus.FAILED;
        } else if (this.status == PaymentStatus.CONFIRMED) {
            throw new InvalidStateException(ErrorCodes.PAYMENT_ALREADY_FINALIZED, ErrorMessages.PAYMENT_ALREADY_FINALIZED_FAIL);
        }
    }
}

