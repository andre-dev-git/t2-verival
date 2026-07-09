package com.codequests.checkout.order.domain;

import com.codequests.checkout.cart.domain.Cart;
import com.codequests.checkout.cart.domain.CartItem;
import com.codequests.checkout.shared.constants.ErrorCodes;
import com.codequests.checkout.shared.constants.ErrorMessages;
import com.codequests.checkout.shared.domain.BaseEntity;
import com.codequests.checkout.shared.exception.InvalidStateException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "cart_id", nullable = false, unique = true)
    private Cart cart;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.CREATED;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @Version
    private Long version;

    public static Order createFromCart(Cart cart) {
        Order order = new Order();
        order.cart = cart;
        order.status = OrderStatus.CREATED;

        BigDecimal total = BigDecimal.ZERO;
        for (CartItem cartItem : cart.getItems()) {
            BigDecimal unitPrice = cartItem.getProduct().getPrice();
            OrderItem orderItem = new OrderItem(
                    order,
                    cartItem.getProduct(),
                    cartItem.getQuantity(),
                    unitPrice
            );
            order.items.add(orderItem);

            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            total = total.add(lineTotal);
        }
        order.totalAmount = total;

        return order;
    }

    public void startPayment() {
        if (this.status == OrderStatus.PENDING_PAYMENT) {
            return;
        } else if (this.status == OrderStatus.CREATED) {
            this.status = OrderStatus.PENDING_PAYMENT;
        } else if (this.status == OrderStatus.PAYMENT_FAILED) {
            this.status = OrderStatus.PENDING_PAYMENT;
        } else if (this.status == OrderStatus.PAID) {
            throw new InvalidStateException(ErrorCodes.ORDER_ALREADY_PAID, ErrorMessages.ORDER_ALREADY_PAID);
        } else {
            throw new InvalidStateException(ErrorCodes.INVALID_ORDER_STATE_TRANSITION,
                    String.format(ErrorMessages.INVALID_ORDER_STATE_TRANSITION_PAYMENT, this.status));
        }
    }

    public void markPaid() {
        if (this.status == OrderStatus.PAID) {
            return;
        } else if (this.status == OrderStatus.PENDING_PAYMENT) {
            this.status = OrderStatus.PAID;
        } else {
            throw new InvalidStateException(ErrorCodes.INVALID_ORDER_STATE_TRANSITION,
                    String.format(ErrorMessages.INVALID_ORDER_STATE_TRANSITION_PAID, this.status));
        }
    }

    public void markPaymentFailed() {
        if (this.status == OrderStatus.PAYMENT_FAILED) {
            return;
        } else if (this.status == OrderStatus.PENDING_PAYMENT) {
            this.status = OrderStatus.PAYMENT_FAILED;
        } else if (this.status == OrderStatus.PAID) {
            throw new InvalidStateException(ErrorCodes.INVALID_ORDER_STATE_TRANSITION, ErrorMessages.CANNOT_MARK_PAID_ORDER_FAILED);
        } else {
            throw new InvalidStateException(ErrorCodes.INVALID_ORDER_STATE_TRANSITION,
                    String.format(ErrorMessages.INVALID_ORDER_STATE_TRANSITION_FAILED, this.status));
        }
    }
}

