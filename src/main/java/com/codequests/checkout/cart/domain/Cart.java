package com.codequests.checkout.cart.domain;

import com.codequests.checkout.product.domain.Product;
import com.codequests.checkout.shared.constants.ErrorCodes;
import com.codequests.checkout.shared.constants.ErrorMessages;
import com.codequests.checkout.shared.domain.BaseEntity;
import com.codequests.checkout.shared.exception.BusinessException;
import com.codequests.checkout.shared.exception.InvalidStateException;
import com.codequests.checkout.shared.exception.NotFoundException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "carts")
@Getter
@Setter
public class Cart extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CartStatus status = CartStatus.OPEN;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> items = new ArrayList<>();

    @Version
    private Long version;

    public void addItem(Product product, int quantity) {
        validateOpen();
        validateQuantity(quantity);
        validateStockAvailability(product, quantity);

        CartItem existingItem = findItemByProduct(product);
        if (existingItem != null) {
            int newQuantity = existingItem.getQuantity() + quantity;
            validateStockAvailability(product, newQuantity);
            existingItem.setQuantity(newQuantity);
        } else {
            CartItem newItem = new CartItem(this, product, quantity);
            items.add(newItem);
        }
    }

    public void updateItemQuantity(Long itemId, int quantity) {
        validateOpen();

        CartItem item = findItemById(itemId);

        if (quantity == 0) {
            items.remove(item);
        } else {
            validateQuantity(quantity);
            validateStockAvailability(item.getProduct(), quantity);
            item.setQuantity(quantity);
        }
    }

    public void removeItem(Long itemId) {
        validateOpen();
        CartItem item = findItemById(itemId);
        items.remove(item);
    }

    public void markCheckedOut() {
        validateOpen();
        this.status = CartStatus.CHECKED_OUT;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    private void validateOpen() {
        if (this.status != CartStatus.OPEN) {
            throw new InvalidStateException(ErrorCodes.CART_ALREADY_CHECKED_OUT, ErrorMessages.CART_ALREADY_CHECKED_OUT);
        }
    }

    private void validateQuantity(int quantity) {
        if (quantity < 0) {
            throw new BusinessException(ErrorCodes.INVALID_QUANTITY, ErrorMessages.INVALID_QUANTITY_NEGATIVE);
        }
    }

    private void validateStockAvailability(Product product, int requestedQuantity) {
        if (requestedQuantity > product.getAvailableQuantity()) {
            throw new BusinessException(ErrorCodes.INSUFFICIENT_STOCK,
                    String.format(ErrorMessages.INSUFFICIENT_STOCK,
                            product.getName(), product.getAvailableQuantity(), requestedQuantity)
            );
        }
    }

    private CartItem findItemByProduct(Product product) {
        return items.stream()
                .filter(item -> item.getProduct().getId().equals(product.getId()))
                .findFirst()
                .orElse(null);
    }

    private CartItem findItemById(Long itemId) {
        return items.stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException(ErrorCodes.CART_ITEM_NOT_FOUND,
                        String.format(ErrorMessages.CART_ITEM_NOT_FOUND, itemId)
                ));
    }
}

