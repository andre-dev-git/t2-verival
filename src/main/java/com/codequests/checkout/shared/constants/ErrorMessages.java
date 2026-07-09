package com.codequests.checkout.shared.constants;

public final class ErrorMessages {

    private ErrorMessages() {
    }

    public static final String CART_NOT_FOUND = "Cart with id %d not found";
    public static final String CART_ITEM_NOT_FOUND = "Cart item with id %d not found";
    public static final String CART_ALREADY_CHECKED_OUT = "Cannot modify cart after checkout";
    public static final String EMPTY_CART = "Cannot checkout an empty cart";

    public static final String PRODUCT_NOT_FOUND = "Product with id %d not found";
    public static final String INSUFFICIENT_STOCK = "Insufficient stock for product '%s'. Available: %d, Requested: %d";
    public static final String INVALID_QUANTITY = "Quantity must be greater than zero";
    public static final String INVALID_QUANTITY_NEGATIVE = "Quantity cannot be negative";

    public static final String ORDER_NOT_FOUND = "Order with id %d not found";
    public static final String ORDER_ALREADY_PAID = "Cannot start payment for an already paid order";
    public static final String INVALID_ORDER_STATE_TRANSITION_PAYMENT = "Cannot start payment from status %s";
    public static final String INVALID_ORDER_STATE_TRANSITION_PAID = "Cannot mark order as paid from status %s";
    public static final String INVALID_ORDER_STATE_TRANSITION_FAILED = "Cannot mark payment as failed from status %s";
    public static final String CANNOT_MARK_PAID_ORDER_FAILED = "Cannot mark a paid order as failed";

    public static final String PAYMENT_NOT_FOUND = "Payment with id %d not found";
    public static final String PAYMENT_ALREADY_FINALIZED_CONFIRM = "Cannot confirm a failed payment";
    public static final String PAYMENT_ALREADY_FINALIZED_FAIL = "Cannot fail a confirmed payment";
}

