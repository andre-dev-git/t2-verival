package com.codequests.checkout.order.mapper;

import com.codequests.checkout.order.domain.Order;
import com.codequests.checkout.order.domain.OrderItem;
import com.codequests.checkout.order.dto.OrderItemResponse;
import com.codequests.checkout.order.dto.OrderResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;

import java.math.BigDecimal;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface OrderMapper {

    @Mapping(target = "cartId", source = "cart.id")
    OrderResponse toResponse(Order order);

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "totalItemPrice", source = "item", qualifiedByName = "calculateTotalItemPrice")
    OrderItemResponse toItemResponse(OrderItem item);

    @Named("calculateTotalItemPrice")
    default BigDecimal calculateTotalItemPrice(OrderItem item) {
        return item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
    }
}

