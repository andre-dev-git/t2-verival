package com.codequests.checkout.shared.constants;

public final class ErrorCodes {

    private ErrorCodes() {
    }

    public static final String CART_NOT_FOUND = "CART_NOT_FOUND";
    public static final String CART_ITEM_NOT_FOUND = "CART_ITEM_NOT_FOUND";
    public static final String CART_ALREADY_CHECKED_OUT = "CART_ALREADY_CHECKED_OUT";
    public static final String EMPTY_CART = "EMPTY_CART";

    public static final String PRODUCT_NOT_FOUND = "PRODUCT_NOT_FOUND";
    public static final String INSUFFICIENT_STOCK = "INSUFFICIENT_STOCK";
    public static final String INVALID_QUANTITY = "INVALID_QUANTITY";

    public static final String ORDER_NOT_FOUND = "ORDER_NOT_FOUND";
    public static final String ORDER_ALREADY_PAID = "ORDER_ALREADY_PAID";
    public static final String INVALID_ORDER_STATE_TRANSITION = "INVALID_ORDER_STATE_TRANSITION";

    public static final String PAYMENT_NOT_FOUND = "PAYMENT_NOT_FOUND";
    public static final String PAYMENT_ALREADY_FINALIZED = "PAYMENT_ALREADY_FINALIZED";
}

