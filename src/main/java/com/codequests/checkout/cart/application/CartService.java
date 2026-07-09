package com.codequests.checkout.cart.application;

import com.codequests.checkout.cart.domain.Cart;
import com.codequests.checkout.cart.dto.AddCartItemRequest;
import com.codequests.checkout.cart.dto.CartResponse;
import com.codequests.checkout.cart.dto.UpdateCartItemRequest;
import com.codequests.checkout.cart.mapper.CartMapper;
import com.codequests.checkout.cart.repository.CartRepository;
import com.codequests.checkout.product.application.ProductService;
import com.codequests.checkout.product.domain.Product;
import com.codequests.checkout.shared.constants.ErrorCodes;
import com.codequests.checkout.shared.constants.ErrorMessages;
import com.codequests.checkout.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final ProductService productService;
    private final CartMapper cartMapper;

    @Transactional
    public CartResponse createCart() {
        Cart cart = new Cart();
        cart = saveCart(cart);
        return cartMapper.toResponse(cart);
    }

    @Transactional(readOnly = true)
    public CartResponse getCart(Long cartId) {
        Cart cart = findCart(cartId);
        return cartMapper.toResponse(cart);
    }

    @Transactional
    public CartResponse addItem(Long cartId, AddCartItemRequest request) {
        Cart cart = findCart(cartId);
        Product product = productService.findProduct(request.getProductId());

        cart.addItem(product, request.getQuantity());
        cart = saveCart(cart);

        return cartMapper.toResponse(cart);
    }

    @Transactional
    public CartResponse updateItem(Long cartId, Long itemId, UpdateCartItemRequest request) {
        Cart cart = findCart(cartId);

        cart.updateItemQuantity(itemId, request.getQuantity());
        cart = saveCart(cart);

        return cartMapper.toResponse(cart);
    }

    @Transactional
    public void removeItem(Long cartId, Long itemId) {
        Cart cart = findCart(cartId);
        cart.removeItem(itemId);
        saveCart(cart);
    }

    public Cart findCart(Long cartId) {
        return cartRepository.findById(cartId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCodes.CART_NOT_FOUND,
                        String.format(ErrorMessages.CART_NOT_FOUND, cartId)
                ));
    }

    public Cart saveCart(Cart cart) {
        return cartRepository.save(cart);
    }
}

