package com.codequests.checkout.order.application;

import com.codequests.checkout.cart.application.CartService;
import com.codequests.checkout.cart.domain.Cart;
import com.codequests.checkout.cart.domain.CartItem;
import com.codequests.checkout.order.domain.Order;
import com.codequests.checkout.order.dto.OrderResponse;
import com.codequests.checkout.order.mapper.OrderMapper;
import com.codequests.checkout.order.repository.OrderRepository;
import com.codequests.checkout.product.application.ProductService;
import com.codequests.checkout.product.domain.Product;
import com.codequests.checkout.shared.constants.ErrorCodes;
import com.codequests.checkout.shared.constants.ErrorMessages;
import com.codequests.checkout.shared.exception.BusinessException;
import com.codequests.checkout.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductService productService;
    private final CartService cartService;

    private final OrderMapper orderMapper;

    @Transactional
    public OrderResponse checkout(Long cartId) {
        return orderRepository.findByCartId(cartId)
                .map(orderMapper::toResponse)
                .orElseGet(() -> createNewOrder(cartId));
    }

    private OrderResponse createNewOrder(Long cartId) {
        Cart cart = cartService.findCart(cartId);

        if (cart.isEmpty()) {
            throw new BusinessException(ErrorCodes.EMPTY_CART, ErrorMessages.EMPTY_CART);
        }

        List<Product> products = new ArrayList<>();
        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();
            product.subtractStock(cartItem.getQuantity());
            products.add(product);
        }

        productService.saveAllProducts(products);

        Order order = Order.createFromCart(cart);
        cart.markCheckedOut();
        cartService.saveCart(cart);

        order = saveOrder(order);

        return orderMapper.toResponse(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long orderId) {
        Order order = findOrder(orderId);
        return orderMapper.toResponse(order);
    }

    public Order findOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.ORDER_NOT_FOUND, String.format(ErrorMessages.ORDER_NOT_FOUND, orderId)));
    }
    public Order saveOrder(Order order) {
        return orderRepository.save(order);
    }
}

