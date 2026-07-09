package com.codequests.checkout.cart.domain;

import com.codequests.checkout.product.domain.Product;
import com.codequests.checkout.shared.exception.BusinessException;
import com.codequests.checkout.shared.exception.InvalidStateException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Cart Domain Tests")
class CartDomainTest {

    @Test
    @DisplayName("Cannot modify checked-out cart")
    void cannotModifyCheckedOutCart() {
        Cart cart = new Cart();
        Product product = createProduct(1L, "Test Product", 100);

        cart.addItem(product, 1);
        cart.markCheckedOut();

        assertThatThrownBy(() -> cart.addItem(product, 1))
                .isInstanceOf(InvalidStateException.class)
                .hasMessageContaining("Cannot modify cart after checkout");
    }

    @Test
    @DisplayName("Adding same product twice merges quantity")
    void addingSameProductMergesQuantity() {
        Cart cart = new Cart();
        Product product = createProduct(1L, "Test Product", 100);

        cart.addItem(product, 2);
        cart.addItem(product, 3);

        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(5);
    }

    @Test
    @DisplayName("Updating quantity to zero removes item")
    void updatingQuantityToZeroRemovesItem() {
        Cart cart = new Cart();
        Product product = createProduct(1L, "Test Product", 100);

        cart.addItem(product, 5);
        cart.getItems().get(0).setId(1L);
        cart.updateItemQuantity(1L, 0);

        assertThat(cart.getItems()).isEmpty();
    }

    @Test
    @DisplayName("Cannot add negative quantity")
    void cannotAddNegativeQuantity() {
        Cart cart = new Cart();
        Product product = createProduct(1L, "Test Product", 100);

        assertThatThrownBy(() -> cart.addItem(product, -1))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Quantity cannot be negative");
    }

    @Test
    @DisplayName("Cannot add quantity exceeding stock")
    void cannotAddQuantityExceedingStock() {
        Cart cart = new Cart();
        Product product = createProduct(1L, "Test Product", 5);

        assertThatThrownBy(() -> cart.addItem(product, 10))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Insufficient stock");
    }

    @Test
    @DisplayName("C4 - Can add quantity exactly equal to available stock (upper boundary)")
    void canAddQuantityEqualToStock() {
        Cart cart = new Cart();
        Product product = createProduct(1L, "Test Product", 10);

        // Limite superior válido: quantidade == estoque disponível deve ser aceita
        cart.addItem(product, 10);

        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(10);
    }

    @Test
    @DisplayName("C5 - Cannot add quantity of stock plus one (just above boundary)")
    void cannotAddQuantityJustAboveStock() {
        Cart cart = new Cart();
        Product product = createProduct(1L, "Test Product", 10);

        // Logo acima do limite: estoque + 1 deve ser rejeitado
        assertThatThrownBy(() -> cart.addItem(product, 11))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Insufficient stock");
    }

    @Test
    @DisplayName("Empty cart returns true for isEmpty")
    void emptyCartReturnsTrue() {
        Cart cart = new Cart();

        assertThat(cart.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("Cart with items returns false for isEmpty")
    void cartWithItemsReturnsFalse() {
        Cart cart = new Cart();
        Product product = createProduct(1L, "Test Product", 10);

        cart.addItem(product, 1);

        assertThat(cart.isEmpty()).isFalse();
    }

    private Product createProduct(Long id, String name, int availableQuantity) {
        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setPrice(BigDecimal.valueOf(100));
        product.setAvailableQuantity(availableQuantity);
        return product;
    }
}

