package com.codequests.checkout.cart.api;

import com.codequests.checkout.cart.application.CartService;
import com.codequests.checkout.cart.dto.AddCartItemRequest;
import com.codequests.checkout.cart.dto.CartResponse;
import com.codequests.checkout.cart.dto.UpdateCartItemRequest;
import com.codequests.checkout.order.application.OrderService;
import com.codequests.checkout.order.dto.OrderResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/carts")
@RequiredArgsConstructor
@Tag(name = "Carts", description = "Shopping cart operations")
public class CartController {

    private final CartService cartService;
    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "Create a new cart")
    public ResponseEntity<CartResponse> createCart() {
        return ResponseEntity.status(HttpStatus.CREATED).body(cartService.createCart());
    }

    @GetMapping("/{cartId}")
    @Operation(summary = "Get cart by ID")
    public ResponseEntity<CartResponse> getCart(@PathVariable Long cartId) {
        return ResponseEntity.ok(cartService.getCart(cartId));
    }

    @PostMapping("/{cartId}/items")
    @Operation(summary = "Add item to cart")
    public ResponseEntity<CartResponse> addItem(
            @PathVariable Long cartId,
            @Valid @RequestBody AddCartItemRequest request) {
        return ResponseEntity.ok(cartService.addItem(cartId, request));
    }

    @PutMapping("/{cartId}/items/{itemId}")
    @Operation(summary = "Update cart item quantity")
    public ResponseEntity<CartResponse> updateItem(
            @PathVariable Long cartId,
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        return ResponseEntity.ok(cartService.updateItem(cartId, itemId, request));
    }

    @DeleteMapping("/{cartId}/items/{itemId}")
    @Operation(summary = "Remove item from cart")
    public ResponseEntity<Void> removeItem(
            @PathVariable Long cartId,
            @PathVariable Long itemId) {
        cartService.removeItem(cartId, itemId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{cartId}/checkout")
    @Operation(summary = "Checkout cart to create order")
    public ResponseEntity<OrderResponse> checkout(@PathVariable Long cartId) {
        return ResponseEntity.ok(orderService.checkout(cartId));
    }
}

