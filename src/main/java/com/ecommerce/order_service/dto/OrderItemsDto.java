package com.ecommerce.order_service.dto;

import lombok.Data;

@Data
public class OrderItemsDto {
    private String productId;
    private Integer unitPrice;
    private String deliveredKey;
}
