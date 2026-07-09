package com.codequests.checkout.product.application;

import com.codequests.checkout.product.domain.Product;
import com.codequests.checkout.product.dto.ProductResponse;
import com.codequests.checkout.product.mapper.ProductMapper;
import com.codequests.checkout.product.repository.ProductRepository;
import com.codequests.checkout.shared.constants.ErrorCodes;
import com.codequests.checkout.shared.constants.ErrorMessages;
import com.codequests.checkout.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    public List<ProductResponse> getProducts() {
        List<Product> products = productRepository.findAll();
        return productMapper.toResponseList(products);
    }

    public ProductResponse getProduct(Long productId) {
        Product product = findProduct(productId);
        return productMapper.toResponse(product);
    }

    public Product findProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.PRODUCT_NOT_FOUND, String.format(ErrorMessages.PRODUCT_NOT_FOUND, productId)));
    }

    public void saveAllProducts(List<Product> products) {
         productRepository.saveAll(products);
    }
}

