package com.codequests.checkout.product.domain;

import com.codequests.checkout.shared.constants.ErrorCodes;
import com.codequests.checkout.shared.constants.ErrorMessages;
import com.codequests.checkout.shared.domain.BaseEntity;
import com.codequests.checkout.shared.exception.BusinessException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "products")
@Getter
@Setter
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private int availableQuantity;

    @Version
    private Long version;

    public void subtractStock(int quantity) {
        if (quantity <= 0) {
            throw new BusinessException(ErrorCodes.INVALID_QUANTITY, ErrorMessages.INVALID_QUANTITY);
        }
        if (this.availableQuantity < quantity) {
            throw new BusinessException(
                    ErrorCodes.INSUFFICIENT_STOCK,
                    String.format(ErrorMessages.INSUFFICIENT_STOCK, this.name, this.availableQuantity, quantity)
            );
        }
        this.availableQuantity -= quantity;
    }
}

