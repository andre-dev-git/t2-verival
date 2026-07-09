package com.codequests.checkout.product.mapper;

import com.codequests.checkout.product.domain.Product;
import com.codequests.checkout.product.dto.ProductResponse;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ProductMapper {
    ProductResponse toResponse(Product product);
    List<ProductResponse> toResponseList(List<Product> products);
}

