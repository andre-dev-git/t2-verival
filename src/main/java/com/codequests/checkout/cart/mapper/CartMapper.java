package com.codequests.checkout.cart.mapper;

import com.codequests.checkout.cart.domain.Cart;
import com.codequests.checkout.cart.domain.CartItem;
import com.codequests.checkout.cart.dto.CartItemResponse;
import com.codequests.checkout.cart.dto.CartResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CartMapper {

    @Mapping(target = "totalItems", source = "items", qualifiedByName = "calculateTotalItems")
    CartResponse toResponse(Cart cart);

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "currentUnitPrice", source = "product.price")
    CartItemResponse toItemResponse(CartItem item);

    @Named("calculateTotalItems")
    default int calculateTotalItems(List<CartItem> items) {
        return items.stream()
                .mapToInt(CartItem::getQuantity)
                .sum();
    }
}

