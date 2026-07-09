package com.codequests.checkout.cart.dto;

import com.codequests.checkout.cart.domain.CartStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CartResponse {
    private Long id;
    private CartStatus status;
    private List<CartItemResponse> items;
    private int totalItems;
}

